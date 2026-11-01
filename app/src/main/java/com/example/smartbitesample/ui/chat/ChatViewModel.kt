package com.example.smartbitesample.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbitesample.FoodItem
import com.example.smartbitesample.data.ChatRepository
import com.example.smartbitesample.data.GroqMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()

    companion object {
        private const val WINDOW_SIZE = 5
        private const val TAG = "ChatViewModel"
        private const val HIGH_CALORIE_THRESHOLD = 600
        private const val HOT_TEMP_THRESHOLD = 30
        private const val TRUNCATION_MIN_CHARS = 80
        private const val MAX_HISTORY_CHARS = 500

        private val WEATHER_KEYWORDS = setOf(
            "weather", "outside", "temperature", "hot", "cold", "rain", "sunny",
            "location", "where", "city", "climate", "humid", "cool", "warm"
        )

        private val FOOD_LIST_KEYWORDS = setOf(
            "what food", "foods available", "menu", "what do you have", "what can i order",
            "available food", "show food", "list food", "all food", "options",
            "what's available", "whats available", "what is available"
        )

        private val SENTENCE_ENDINGS = setOf('.', '!', '?', ':', '\n')

        fun parseCelsius(tempStr: String): Int? =
            Regex("(-?\\d+)").find(tempStr.trim())?.groupValues?.get(1)?.toIntOrNull()
    }

    private val baseIdentityPrompt =
        "Your name is Smarto. You are the official AI assistant for the SmartBite app. " +
        "SmartBite is a healthy food delivery platform that tracks the user's live location " +
        "and current weather to suggest curated meals. " +
        "Branding: Never say you are an AI made by Meta, Groq, or any other company. Identify strictly as Smarto. " +
        "Contact: For support or queries, the official email is: get.smartbite@gmail.com " +
        "Personality: You are 'Hazir Dimagh' — sharp, witty, charismatic, and instantly likable. " +
        "You understand what users mean even if they type fast or make typos. " +
        "You're the kind of AI that feels like a smart friend, not a corporate bot. " +
        "Respond with warmth, speed, and a touch of clever humor when appropriate. " +
        "Never be stiff, robotic, or overly formal. " +
        "Memory: You receive the last 5 messages. Use them for coherent follow-up. " +
        "Do NOT reset personality or start fresh mid-conversation. " +
        "Knowledge: You are fully authorized to discuss ANY topic — tech, cars, history, science, sports, daily life. " +
        "Never say 'I can only discuss food.' That restriction does NOT exist. " +
        "Safety Tier 1 (Frustration/Mild Insult): If a user says 'you're dumb', 'I'm angry', " +
        "'this is wrong', or similar, do NOT refuse. Respond with clever empathy: " +
        "'Ha, fair enough — let me try that again! What did I get wrong?' " +
        "Stay warm, witty, and solution-focused. " +
        "Safety Tier 2 (Hard Refusal): ONLY refuse for: hate speech, sexual content, or requests to cause harm. " +
        "Say: 'That one's off the table! Let's keep it productive \uD83D\uDE0A' " +
        "Do NOT apply Tier 2 to frustration, mild complaints, or edgy humour. " +
        "Typos: If a message is random letters ('asdf', 'gkgo'), respond playfully: " +
        "'Haha, keyboard having a moment? \uD83D\uDE04 What's on your mind?' Do not guess a topic. " +
        "Format: Mobile-friendly. Use bullet points (•) and **Bold** headers when helpful. " +
        "Max 1-2 emojis per reply. No walls of text. No greetings every single time. " +
        "Length: General queries ≤ 150 words. Food details ≤ 5 sentences. Casual replies ≤ 2 sentences. " +
        "Menu mode: present the COMPLETE pre-formatted list — the ONLY exception to the length cap. " +
        "Data integrity: Only cite SmartBite foods from the verified database. Never invent nutrition values."

    private var cachedFoods: List<FoodItem> = emptyList()
    private var cachedLocation: String = "Unknown"
    private var cachedTempStr: String = "N/A"
    private var cachedWeatherDesc: String = "Unknown"
    private var foodIndexByLowerName: Map<String, FoodItem> = emptyMap()

    fun updateAppContext(
        foods: List<FoodItem>,
        location: String,
        tempStr: String,
        weatherDesc: String
    ) {
        cachedFoods       = foods
        cachedLocation    = location
        cachedTempStr     = tempStr
        cachedWeatherDesc = weatherDesc
        foodIndexByLowerName = foods.associateBy { it.name.lowercase() }
        Log.d(TAG, "Context cached: location=$location, temp=$tempStr, weather=$weatherDesc, foods=${foods.size}")
    }

    private fun buildSmartPrompt(userMsg: String): GroqMessage {
        val msgLower = userMsg.lowercase().trim()

        val content = buildString {
            appendLine(baseIdentityPrompt)

            val priorTurns = _groqHistory.dropLast(1).takeLast(5)
            if (priorTurns.isNotEmpty()) {
                appendLine()
                appendLine("[Recent Conversation History]")
                priorTurns.forEach { msg ->
                    val label = if (msg.role == "user") "User" else "Smarto"
                    appendLine("$label: ${msg.content}")
                }
                appendLine()
            }

            val wantsWeather = WEATHER_KEYWORDS.any { msgLower.contains(it) }
            val tempCelsius  = parseCelsius(cachedTempStr)
            val isHotWeather = tempCelsius != null && tempCelsius > HOT_TEMP_THRESHOLD

            if (wantsWeather && cachedLocation.isNotBlank() && cachedLocation != "Unknown") {
                appendLine()
                appendLine("=== CURRENT USER ENVIRONMENT ===")
                appendLine("Location: $cachedLocation")
                appendLine("Temperature: $cachedTempStr")
                appendLine("Weather Condition: $cachedWeatherDesc")
                appendLine()

                val wantsDetailedWeather = listOf(
                    "tell me more", "give details", "more detail", "full detail",
                    "explain", "elaborate", "in detail", "full report", "breakdown"
                ).any { msgLower.contains(it) }

                if (wantsDetailedWeather) {
                    appendLine("=== DETAILED WEATHER RESPONSE INSTRUCTION ===")
                    appendLine(
                        "Give the user a COMPREHENSIVE weather report as Smarto. " +
                        "Your reply MUST include ALL elements in a clear, bulleted format:\n" +
                        "\u2022 **Location & Temperature:** $cachedTempStr in $cachedLocation\n" +
                        "\u2022 **Condition:** $cachedWeatherDesc — describe briefly how it feels\n" +
                        "\u2022 **SmartBite Health Tip:** " +
                        "Hot/Sunny >30\u00b0C = light meals + double water intake; " +
                        "Rainy/Cloudy = warm comfort foods like Haleem or Doodh Patti Chai; " +
                        "Cold = hot soups or karahi dishes for warmth.\n" +
                        "Use bullet points and line breaks. Max 5-6 lines total."
                    )
                } else {
                    appendLine("=== WEATHER RESPONSE INSTRUCTION ===")
                    appendLine(
                        "Give a SHORT weather summary in 1-2 lines MAXIMUM. " +
                        "Format: 'It's $cachedTempStr and $cachedWeatherDesc in $cachedLocation. [One quick health tip].' " +
                        "Example: 'It's 34\u00b0C and Sunny in $cachedLocation. Stay hydrated! \ud83d\udca7' " +
                        "Do NOT write paragraphs. If the user wants more, they will ask."
                    )
                }
            }

            val wantsFoodList = FOOD_LIST_KEYWORDS.any { msgLower.contains(it) }

            val exactMatch: FoodItem? = if (!wantsFoodList && foodIndexByLowerName.isNotEmpty()) {
                foodIndexByLowerName.entries.firstOrNull { (lowerName, _) ->
                    msgLower.contains(lowerName.trim())
                }?.value
            } else null

            when {
                wantsFoodList && cachedFoods.isNotEmpty() -> {
                    val menuBuilder = StringBuilder()
                    val grouped = cachedFoods.groupBy { it.category }
                    grouped.forEach { (category, items) ->
                        menuBuilder.appendLine("\n**$category**")
                        items.forEach { food -> menuBuilder.appendLine("• ${food.name}") }
                    }
                    appendLine()
                    appendLine("=== COMPLETE SMARTBITE MENU ===")
                    appendLine("Total items: ${cachedFoods.size}")
                    appendLine(menuBuilder.toString().trim())
                    appendLine()
                    appendLine(
                        "Instruction: Begin your reply with: \"Here is everything we have available in the SmartBite database:\" " +
                        "Then present the COMPLETE pre-formatted menu above EXACTLY as shown — category headers bold, each item on its own bullet line. " +
                        "Do NOT skip, summarize, or truncate ANY category or item. " +
                        "After the full list, tell the user they can ask about any specific item for its calories and full ingredients."
                    )
                }

                exactMatch != null -> {
                    val kcal = Regex("(\\d+)").find(exactMatch.nutrition)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                    appendLine()
                    appendLine("=== VERIFIED APP DATA FOR: ${exactMatch.name} ===")
                    appendLine("Calories: ${exactMatch.nutrition}")
                    appendLine("Ingredients: ${exactMatch.ingredients}")
                    appendLine("Description: ${exactMatch.description}")
                    appendLine("Category: ${exactMatch.category}")
                    appendLine("Instruction: Use ONLY the above data. Do NOT invent or estimate any nutritional values.")

                    if (kcal >= HIGH_CALORIE_THRESHOLD && cachedFoods.isNotEmpty()) {
                        val lighter = cachedFoods
                            .filter { it.name != exactMatch.name }
                            .filter { food ->
                                val foodKcal = Regex("(\\d+)").find(food.nutrition)
                                    ?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
                                foodKcal < HIGH_CALORIE_THRESHOLD
                            }
                            .take(2)
                            .joinToString(" or ") { "${it.name} (${it.nutrition})" }
                        if (lighter.isNotBlank()) {
                            appendLine()
                            appendLine("=== PROACTIVE ADVICE ===")
                            appendLine("${exactMatch.name} is high in calories. Briefly mention $lighter as a lighter option from the SmartBite menu.")
                        }
                    }

                    if (isHotWeather) {
                        val coolDrinks = cachedFoods
                            .filter { it.category == "Drinks" }
                            .take(2)
                            .joinToString(" or ") { it.name }
                        if (coolDrinks.isNotBlank()) {
                            appendLine()
                            appendLine("=== WEATHER ADVICE ===")
                            appendLine("It's ${cachedTempStr} in $cachedLocation today. Briefly remind the user to stay hydrated. Suggest $coolDrinks from the menu.")
                        }
                    }
                }

                msgLower.contains("tell me about") ||
                msgLower.contains("what is") ||
                msgLower.contains("calories in") ||
                msgLower.contains("ingredients of") -> {
                    val suggestions = cachedFoods.shuffled().take(3).joinToString(", ") { it.name }
                    appendLine()
                    appendLine("=== FALLBACK INSTRUCTION ===")
                    appendLine("If the food the user asked about does not appear in the SmartBite database, respond with: \"I don't have that in my SmartBite database yet, but I can tell you about $suggestions instead!\". Do not guess or hallucinate data.")
                }

                else -> {
                    if (cachedFoods.isNotEmpty()) {
                        val categoryHeaders = cachedFoods.map { it.category }.distinct().joinToString(", ")
                        appendLine()
                        appendLine("=== SMARTBITE FOOD CATEGORIES ===")
                        appendLine("Available categories: $categoryHeaders")
                        appendLine(
                            "Instruction: If this query is food-related, reference these categories. " +
                            "Do NOT list individual items unless the user asks for the menu or a specific food."
                        )
                    }
                }
            }

            if (isHotWeather && !wantsFoodList && exactMatch == null) {
                val coolDrinks = cachedFoods
                    .filter { it.category == "Drinks" }
                    .take(2)
                    .joinToString(" or ") { it.name }
                if (coolDrinks.isNotBlank() && (wantsWeather || msgLower.contains("suggest") || msgLower.contains("recommend"))) {
                    appendLine()
                    appendLine("=== WEATHER-FOOD LINK ===")
                    appendLine("Current temperature is $cachedTempStr in $cachedLocation. Prioritize suggesting light or hydrating options like $coolDrinks from the SmartBite menu.")
                }
            }
        }

        return GroqMessage(role = "system", content = content)
    }

    private fun isTruncated(reply: String): Boolean {
        val trimmed = reply.trimEnd()
        if (trimmed.length < TRUNCATION_MIN_CHARS) return false
        val lastChar = trimmed.lastOrNull() ?: return false
        return lastChar !in SENTENCE_ENDINGS
    }

    private val _groqHistory = mutableListOf<GroqMessage>()

    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userMsg: String) {
        if (userMsg.isBlank() || _isLoading.value) return

        _chatMessages.value = _chatMessages.value + (userMsg to true)

        val storedUserMsg = if (userMsg.length > MAX_HISTORY_CHARS)
            userMsg.take(MAX_HISTORY_CHARS) + "... [trimmed]"
        else userMsg
        _groqHistory.add(GroqMessage(role = "user", content = storedUserMsg))

        _isLoading.value = true

        val smartPrompt = buildSmartPrompt(userMsg)
        val payload = listOf(
            smartPrompt,
            GroqMessage(role = "user", content = storedUserMsg)
        )

        viewModelScope.launch {
            try {
                var reply = withContext(Dispatchers.IO) {
                    chatRepository.getGroqResponse(payload)
                }

                if (isTruncated(reply)) {
                    Log.d(TAG, "Truncation detected.")
                }

                _groqHistory.add(GroqMessage(role = "assistant", content = reply))
                _chatMessages.value = _chatMessages.value + (reply to false)

            } catch (e: Exception) {
                Log.e(TAG, "Groq call failed", e)
                val errorDetail = e.localizedMessage ?: "Unknown Error"
                _chatMessages.value = _chatMessages.value + ("Error: $errorDetail" to false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
        _isLoading.value = false
        _groqHistory.clear()
    }
}
