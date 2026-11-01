package com.example.smartbitesample.ui.home
import java.time.LocalTime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.example.smartbitesample.FoodItem
import com.example.smartbitesample.UserEntity
import com.example.smartbitesample.ui.components.MinimalFoodCard
import com.example.smartbitesample.ui.components.ShimmerFoodCard

@Composable
fun HomeScreen(
    user: UserEntity,
    viewModel: HomeViewModel,
    weatherDesc: String,
    temperature: Double,
    formattedTemp: String,
    isLocationDisabled: Boolean,
    locationName: String = "Detecting...",
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    onFoodClick: (FoodItem) -> Unit,
    onFavoriteClick: (FoodItem) -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    favorites: List<String> = emptyList()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredFoods by viewModel.filteredFoods.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val columns = if (screenWidthDp >= 600) 4 else 2

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            Surface(
                color = if (isDark) com.example.smartbitesample.ui.theme.NavFrameCharcoal else MaterialTheme.colors.primary,
                shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp),
                elevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(
                            top = if (screenWidthDp >= 600) 2.dp else 0.dp,
                            bottom = if (screenWidthDp >= 600) 6.dp else 4.dp,
                            start = 20.dp,
                            end = 20.dp
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = if (isDark) com.example.smartbitesample.ui.theme.DeepHeritageOrange else Color.White,
                            modifier = Modifier.size(if (screenWidthDp >= 600) 24.dp else 30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SmartBite",
                            fontSize = if (screenWidthDp >= 600) 26.sp else 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) com.example.smartbitesample.ui.theme.DeepHeritageOrange else Color.White
                        )
                    }

                    if (screenWidthDp < 600) {
                        Spacer(modifier = Modifier.height(2.dp))
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(if (screenWidthDp >= 600) 36.dp else 42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = if (screenWidthDp < 360) 10.dp else 16.dp)
                            ) {
                                // Location Section
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isLocationDisabled) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isDark) MaterialTheme.colors.primary else Color(0xFFFFD54F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Current location",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFFD54F),
                                            textAlign = TextAlign.Center,

                                            modifier = Modifier.offset(y = (-3).dp)
                                        )
                                        Text(
                                            text = if (isLocationDisabled) "Location Disabled" else locationName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.widthIn(max = if (screenWidthDp < 360) 85.dp else 130.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(if (screenWidthDp < 360) 8.dp else 20.dp))
                                Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Color.White.copy(alpha = 0.15f))

                                IconButton(
                                    onClick = { viewModel.fetchLocationAndWeather() },
                                    modifier = Modifier.size(40.dp).padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reload",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Color.White.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.width(if (screenWidthDp < 360) 8.dp else 20.dp))

                                if (!isLocationDisabled) {
                                    val (effectiveWeatherText, weatherIcon) = when (weatherDesc) {
                                        "Fetching..." -> Pair("Fetching...", Icons.Outlined.Cloud)
                                        "Rainy" -> Pair("Rainy", Icons.Outlined.WaterDrop)
                                        "Cloudy" -> Pair("Cloudy", Icons.Outlined.Cloud)
                                        "Clear" -> Pair("Clear", Icons.Outlined.NightsStay)
                                        "Sunny" -> Pair("Sunny", Icons.Outlined.WbSunny)
                                        else -> Pair(weatherDesc, Icons.Outlined.Cloud)
                                    }

                                    Icon(
                                        imageVector = weatherIcon,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = if (weatherDesc == "Fetching...") "Fetching..." else formattedTemp,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = effectiveWeatherText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LocationOff,
                                        contentDescription = "Location Off",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }


                    if (screenWidthDp < 600) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val displayName = if (user.name.isNotBlank()) user.name else "Guest"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (screenWidthDp >= 600) 4.dp else 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (displayName.length > 12) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Welcome",
                                    fontSize = if (screenWidthDp >= 600) 14.sp else 16.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "$displayName!",
                                    fontSize = if (screenWidthDp >= 600) 14.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        } else {
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    append("Welcome ")
                                    withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(displayName)
                                    }
                                    append("!")
                                },
                                fontSize = if (screenWidthDp >= 600) 14.sp else 16.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search dishes...", color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isDark) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.60f), modifier = Modifier.size(20.dp)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp) 
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            backgroundColor = MaterialTheme.colors.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape)
                            .clickable { onThemeToggle() },
                        shape = CircleShape,
                        color = MaterialTheme.colors.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = "Theme Toggle",
                                tint = if (isDark) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LazyRow(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        items(viewModel.categories, key = { it }) { category ->
                            val isSelected = category == selectedCategory
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 5.dp)
                                    .clickable { viewModel.onCategorySelected(category) }
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                border = null
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(filteredFoods, key = { it.id }) { food ->
                        val isFavorite = favorites.contains(food.name)
                        MinimalFoodCard(
                            food = food, 
                            onClick = { onFoodClick(food) },
                            onFavoriteClick = { onFavoriteClick(food) },
                            onAddToCart = { onAddToCart(food) },
                            isFavorite = isFavorite,
                            isDark = isDark
                        )
                    }
                }
            }
        }
    }
}
