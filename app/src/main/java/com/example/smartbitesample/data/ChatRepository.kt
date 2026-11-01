package com.example.smartbitesample.data

import android.util.Log
import com.example.smartbitesample.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class GroqMessage(
    @SerializedName("role")    val role: String,
    @SerializedName("content") val content: String
)

data class GroqRequest(
    @SerializedName("model")                val model: String,
    @SerializedName("messages")             val messages: List<GroqMessage>,
    @SerializedName("max_completion_tokens") val maxCompletionTokens: Int = 800
)

data class GroqChoice(
    @SerializedName("message") val message: GroqMessage
)

data class GroqResponse(
    @SerializedName("choices") val choices: List<GroqChoice>
)

class ChatRepository {

    companion object {
        private const val TAG        = "ChatRepository"
        private const val GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions"
        private const val GROQ_MODEL = "qwen/qwen3.8-27b"
        private val GROQ_API_KEY     = BuildConfig.GROQ_API_KEY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getGroqResponse(history: List<GroqMessage>): String =
        executeRequest(history, isRetry = false)

    private suspend fun executeRequest(history: List<GroqMessage>, isRetry: Boolean): String {
        return try {
            val requestBody = gson.toJson(
                GroqRequest(model = GROQ_MODEL, messages = history, maxCompletionTokens = 800)
            ).toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(GROQ_URL)
                .addHeader("Authorization", "Bearer $GROQ_API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.code == 429) {
                val errBody = response.body?.string() ?: "No body"
                Log.w(TAG, "Groq rate limited (429): $errBody")
                if (!isRetry) {
                    kotlinx.coroutines.delay(3_000)
                    return executeRequest(history, isRetry = true)
                }
                return "Smarto is a little busy right now. Please try again in a moment! \uD83D\uDE4F"
            }

            if (response.code == 413) {
                Log.e(TAG, "Groq payload too large (413)")
                return "That request was a bit too detailed for me to process at once. Try asking about one item at a time!"
            }

            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "No body"
                Log.e(TAG, "Groq API error ${response.code}: $errBody")
                return "Sorry, I'm having trouble connecting right now. (${response.code})"
            }

            val responseBody = response.body?.string()
                ?: return "I couldn't process that. (empty response)"

            val parsed = gson.fromJson(responseBody, GroqResponse::class.java)
            parsed.choices.firstOrNull()?.message?.content?.trim()
                ?: "I couldn't process that. (no choices)"

        } catch (e: Exception) {
            Log.e(TAG, "Groq call failed", e)
            "Error: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}
