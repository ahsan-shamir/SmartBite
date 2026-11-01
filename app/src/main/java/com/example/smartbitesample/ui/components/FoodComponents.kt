package com.example.smartbitesample.ui.components

import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.smartbitesample.FavoriteEntity
import com.example.smartbitesample.FoodItem
import com.example.smartbitesample.ui.theme.ForestEmerald
import com.example.smartbitesample.ui.theme.DeepHeritageOrange

@Composable
fun CategoryItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
        elevation = if (isSelected) 4.dp else 0.dp,
        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (isSelected) Color.White else MaterialTheme.colors.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun FoodCard(
    food: FoodItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column {
            Box(modifier = Modifier.height(135.dp).fillMaxWidth()) {
                AsyncImage(
                    model = food.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Favorite Button Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    elevation = 2.dp
                ) {
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Health Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (food.isHighSugar) HealthBadge("High Sugar", Color(0xFFFF9800))
                    if (food.isHighSalt) HealthBadge("High Salt", Color(0xFFF44336))
                    if (food.isOily) HealthBadge("Oily", Color(0xFF795548))
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = food.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Rs ${food.price.toInt()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colors.primary
                    )
                }
                
                Text(
                    text = food.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "20-30 min", fontSize = 12.sp, color = Color.Gray)
                    }
                    
                    Button(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HealthBadge(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FoodDetailDialog(food: FoodItem, onDismiss: () -> Unit, onAddToCart: (Int) -> Unit) {
    var quantity by remember { mutableIntStateOf(1) }
    val configuration = LocalConfiguration.current
    val isLaptop = configuration.screenWidthDp >= 600

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .statusBarsPadding()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colors.surface,
            elevation = 16.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            if (isLaptop) {
                // Laptop Horizontal Layout
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Pane: Immersive Visual (50%)
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
                        AsyncImage(
                            model = food.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        )
                    }

                    // Right Pane: Detailed Content (50%)
                    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .background(Color(0xFF1E1E1E).copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White)
                            }
                        }
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 32.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp)) // Reduced top spacing since X is now here
                            
                            FoodDetailHeader(food)
                            
                            Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))

                            FoodDetailSections(food)

                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // Sticky Action Bar (Right Pane Only)
                        FoodDetailActionBar(
                            food = food,
                            quantity = quantity,
                            onQuantityChange = { quantity = it },
                            onAddToCart = { onAddToCart(it) }
                        )
                    }
                }
            } else {
                // Mobile Vertical Layout
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Image + Close button — fixed at top
                    Box {
                        AsyncImage(
                            model = food.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color(0xFF1E1E1E).copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }

                    // Main content area — Scrollable
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        FoodDetailHeader(food)
                        
                        Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))

                        FoodDetailSections(food)

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Bottom Bar (Full Width for Mobile)
                    FoodDetailActionBar(
                        food = food,
                        quantity = quantity,
                        onQuantityChange = { quantity = it },
                        onAddToCart = { onAddToCart(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodDetailHeader(food: FoodItem) {
    // 1 & 2. Food Name & Description (Tight Group)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = food.name, 
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold, 
            color = MaterialTheme.colors.primary
        )
        
        Text(
            text = food.description,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = Int.MAX_VALUE
        )
    }

    Row(
        modifier = Modifier.padding(top = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Rs ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = food.price.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
private fun FoodDetailSections(food: FoodItem) {
    if (food.ingredients.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Ingredients:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = food.ingredients,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Divider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp)
        }
    }

    if (food.nutrition.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Nutrition",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(per serving):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                )
            }
            Text(
                text = food.nutrition,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Divider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp)
        }
    }

    if (food.history.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "History:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            Text(
                text = food.history,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Divider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp)
        }
    }
}

@Composable
private fun FoodDetailActionBar(
    food: FoodItem,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: (Int) -> Unit
) {
    val totalPrice by remember(quantity, food.price) { derivedStateOf { (quantity * food.price).toLong() } }
    
    Surface(
        elevation = 8.dp,
        color = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Sticky Quantity Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(36.dp).background(MaterialTheme.colors.onSurface.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = quantity.toString(),
                    modifier = Modifier.padding(horizontal = 20.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(36.dp).background(DeepHeritageOrange, CircleShape)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            Button(
                onClick = { onAddToCart(quantity) },
                modifier = Modifier.height(50.dp).fillMaxWidth(0.8f),
                shape = RoundedCornerShape(25.dp),
                elevation = ButtonDefaults.elevation(2.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = DeepHeritageOrange)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )) {
                            append("Add to Cart")
                        }
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )) {
                            append("  |  Rs $totalPrice")
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun FavoritesDialog(
    favorites: List<FavoriteEntity>,
    onDismiss: () -> Unit,
    onDelete: (FavoriteEntity) -> Unit,
    onAddToCart: (FavoriteEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Favorites", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (favorites.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No favorites yet", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(favorites) { fav ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = fav.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fav.foodName, fontWeight = FontWeight.Bold)
                                    Text("Rs. ${fav.price}", color = MaterialTheme.colors.primary, fontSize = 14.sp)
                                }
                                IconButton(onClick = { onAddToCart(fav) }) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Add to Cart", tint = MaterialTheme.colors.primary)
                                }
                                IconButton(onClick = { onDelete(fav) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                                }
                            }
                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun MinimalFoodCard(
    food: FoodItem,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToCart: () -> Unit,
    isFavorite: Boolean,
    isDark: Boolean
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Box {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(food.imageUrl)
                        .crossfade(true)
                        .size(600, 480)
                        .build(),
                    contentDescription = food.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = food.name,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = food.description,
                fontSize = 11.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rs. ${food.price.toInt()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestEmerald
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                var isClicked by remember { mutableStateOf(false) }
                
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { 
                            onAddToCart()
                            isClicked = true
                        },
                    shape = CircleShape,
                    color = if (isClicked) MaterialTheme.colors.primary.copy(alpha = 0.8f) else MaterialTheme.colors.onSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = if (isClicked) Color.White else (if (isDark) MaterialTheme.colors.primary else MaterialTheme.colors.surface),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                if (isClicked) {
                    LaunchedEffect(Unit) {
                        delay(500)
                        isClicked = false
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerFoodCard(isDark: Boolean) {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF2A2A2A),
            Color(0xFF3A3A3A),
            Color(0xFF2A2A2A)
        )
    } else {
        listOf(
            Color(0xFFE0E0E0),
            Color(0xFFF5F5F5),
            Color(0xFFE0E0E0)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(brush))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brush))
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(brush))
            }
        }
    }
}
