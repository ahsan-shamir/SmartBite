package com.example.smartbitesample.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration
import com.example.smartbitesample.FoodItem
import com.example.smartbitesample.sampleFoods
import com.example.smartbitesample.ui.components.DIETARY_CONSTRAINTS
import com.example.smartbitesample.ui.home.RecommendationUiState
import com.example.smartbitesample.ui.home.SmartRecommendationsViewModel
import com.example.smartbitesample.ui.theme.DeepHeritageOrange
import com.example.smartbitesample.ui.components.DiseaseSelectionDialog
import com.example.smartbitesample.ui.components.MinimalFoodCard

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SmartRecommendationsScreen(
    viewModel: SmartRecommendationsViewModel,
    onDismiss: () -> Unit,
    isDark: Boolean,
    userDiseases: String = "",
    onUpdateDiseases: (String) -> Unit = {},
    onFoodClick: (FoodItem) -> Unit = {},
    onFavoriteClick: (FoodItem) -> Unit = {},
    onAddToCart: (FoodItem) -> Unit = {},
    favorites: List<String> = emptyList(),
    cartItemCount: Int = 0,
    locationName: String = "Local Area",
    weatherDesc: String = "Cloudy",
    allFoods: List<FoodItem> = emptyList()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val initialCartCount = remember { cartItemCount }
    var userManuallyAdded by remember { mutableStateOf(false) }
    val itemAdded = remember(cartItemCount, userManuallyAdded) { 
        cartItemCount > initialCartCount || userManuallyAdded
    }
    
    var diseases by remember { mutableStateOf(userDiseases) }
    var showDiseaseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userDiseases) {
        diseases = userDiseases
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isExpandedScreen = screenWidthDp >= 600
    val columns = if (isExpandedScreen) 4 else 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(enabled = true, onClick = onDismiss)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(32.dp),
            color = if (isDark) Color(0xFF1E1E1E) else Color.White,
            elevation = 16.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // --- HEADER ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Smart", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DeepHeritageOrange)
                    Text(text = "Recommendations", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DeepHeritageOrange, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Personalized AI-driven meal suggestions based on your health profile, allergies, and local weather conditions to keep you fit and healthy.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                val effectiveWeather = weatherDesc

                val statusText = when {
                    locationName == "Location Off"    -> "Location off, please turn your location on"
                    weatherDesc == "Fetching..."      -> "Fetching . . ."
                    weatherDesc == "Error"            -> "Error . . ."
                    locationName.contains("Fetching", ignoreCase = true) -> "Fetching . . ."
                    locationName.contains("Error", ignoreCase = true)    -> "Error . . ."
                    else -> "You are currently located at $locationName and it's $effectiveWeather outside!"
                }

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepHeritageOrange.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // --- SELECTION & REFRESH ROW ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
                            .clickable { showDiseaseDialog = true }
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (diseases.isBlank()) "Select Dietary Constraints" else "Change Dietary Constraints",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.primary,
                                maxLines = 1
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = { viewModel.refreshRecommendations() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                if (showDiseaseDialog) {
                    DiseaseSelectionDialog(
                        selectedDiseases = if (diseases.isBlank()) emptyList() else diseases.split(", "),
                        onDismiss = { showDiseaseDialog = false },
                        onConfirm = { selectedList ->
                            val newDiseases = selectedList.joinToString(", ")
                            diseases = newDiseases
                            onUpdateDiseases(newDiseases)
                            showDiseaseDialog = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- FOOD RECOMMENDATION GRID ---
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState) {
                        is RecommendationUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = DeepHeritageOrange)
                            }
                        }
                        is RecommendationUiState.Success -> {
                            val finalRecommendations = remember(uiState, diseases, weatherDesc, allFoods) {
                                val selectedList = if (diseases.isBlank()) emptyList() else diseases.split(", ")
                                val sourcePool = if (allFoods.isNotEmpty()) allFoods else sampleFoods
                                
                                val diseaseFiltered = sourcePool.filter { food ->
                                    var isSafe = true
                                    if (selectedList.contains("Diabetes") && food.isHighSugar) isSafe = false
                                    if (selectedList.contains("Blood Pressure (BP)") && food.isHighSalt) isSafe = false
                                    if (selectedList.contains("High Cholesterol") && food.isOily) isSafe = false
                                    if (selectedList.contains("Alpha-gal Syndrome") && food.containsMeat) isSafe = false
                                    if (selectedList.contains("Diabetes") && selectedList.contains("Blood Pressure (BP)") && food.isHighSugar && food.isHighSalt) isSafe = false
                                    if (selectedList.contains("Diabetes") && selectedList.contains("High Cholesterol") && food.isHighSugar && food.isOily) isSafe = false
                                    if (selectedList.contains("Diabetes") && selectedList.contains("Alpha-gal Syndrome") &&  food.isHighSugar && food.containsMeat) isSafe = false
                                    if (selectedList.contains("Blood Pressure (BP)") && selectedList.contains("Alpha-gal Syndrome") &&  food.isHighSalt && food.containsMeat) isSafe = false
                                    if (selectedList.contains("Blood Pressure (BP)") && selectedList.contains("High Cholesterol") &&  food.isHighSugar && food.isOily) isSafe = false
                                    if (selectedList.contains("Alpha-gal Syndrome") && selectedList.contains("High Cholesterol") &&  food.containsMeat && food.isOily) isSafe = false
                                    if (selectedList.contains("Diabetes") && selectedList.contains("Blood Pressure (BP)") && selectedList.contains("High Cholesterol") &&  food.isHighSugar && food.isHighSalt && food.isOily) isSafe = false
                                    if (selectedList.contains("Blood Pressure (BP)") && selectedList.contains("High Cholesterol") && selectedList.contains("Alpha-gal Syndrome") &&  food.isHighSalt && food.isOily && food.containsMeat) isSafe = false
                                    if (selectedList.contains("Diabetes") && selectedList.contains("High Cholesterol") && selectedList.contains("Alpha-gal Syndrome") &&  food.isHighSugar && food.isOily && food.containsMeat) isSafe = false
                                    if (selectedList.contains("Diabetes") && selectedList.contains("Blood Pressure (BP)") && selectedList.contains("Alpha-gal Syndrome") &&  food.isHighSugar && food.isHighSalt && food.containsMeat) isSafe = false
                                    if (selectedList.containsAll(DIETARY_CONSTRAINTS) && food.isHighSugar && food.isHighSalt && food.isOily && food.containsMeat) isSafe = false
                                    isSafe
                                }

                                val lowercaseWeather = weatherDesc.lowercase()
                                val isSunny = lowercaseWeather.contains("sun")
                                val isClear = lowercaseWeather.contains("clear")
                                val isRainy = lowercaseWeather.contains("rain") || lowercaseWeather.contains("drizzle") || lowercaseWeather.contains("cloud") || lowercaseWeather.contains("fog") || lowercaseWeather.contains("storm")

                                val weatherFiltered = diseaseFiltered.filter { food ->
                                    val isWarmFood = food.category == "BBQ & Grills" ||
//                                                    food.category == "Biryani & Rice" ||
//                                                    food.category == "Curries & Karahi" ||
                                                    food.name.contains("Pakora", true) || 
                                                    food.name.contains("Samosa", true) || 
                                                    food.name.contains("Chai", true) || 
                                                    food.name.contains("Kahwa", true)
                                    val isColdFood = food.category == "BBQ & Grills" ||
                                                    food.category == "Biryani & Rice" ||
                                                    food.category == "Curries & Karahi" ||
                                            food.name.contains("Pakora", true) ||
                                            food.name.contains("Samosa", true) ||
                                            food.name.contains("Chai", true) ||
                                            food.name.contains("Kahwa", true)

                                    when {
                                        isSunny -> !isWarmFood
                                        isRainy -> isColdFood
                                        isClear -> isWarmFood && !isWarmFood
                                        else -> true
                                    }
                                }

                                if (weatherFiltered.isEmpty()) diseaseFiltered.shuffled().take(6) 
                                else weatherFiltered.shuffled().take(6)
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(finalRecommendations, key = { it.id }) { food ->
                                    val isFavorite = favorites.contains(food.name)
                                    MinimalFoodCard(
                                        food = food,
                                        onClick = { onFoodClick(food) },
                                        onFavoriteClick = { onFavoriteClick(food) },
                                        onAddToCart = {
                                            onAddToCart(food)
                                            userManuallyAdded = true
                                        },
                                        isFavorite = isFavorite,
                                        isDark = isDark
                                    )
                                }
                            }
                        }
                        is RecommendationUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Failed to load recommendations", color = Color.Red)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.width(180.dp).height(44.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = DeepHeritageOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Text(if (itemAdded) "Done" else "Maybe Later", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
