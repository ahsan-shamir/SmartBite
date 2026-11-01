package com.example.smartbitesample.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbitesample.ui.theme.CleanOffWhite
import com.example.smartbitesample.ui.theme.DeepHeritageOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val duration = 2000
        val animationSpec = tween<Float>(durationMillis = duration, easing = LinearEasing)
        
        launch {
            scale.animateTo(1f, animationSpec)
        }
        launch {
            rotation.animateTo(360f, animationSpec)
        }

        delay(2000)
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.RestaurantMenu,
            contentDescription = "App Logo",
            tint = DeepHeritageOrange,
            modifier = Modifier
                .size(120.dp)
                .scale(scale.value)
                .rotate(rotation.value)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SmartBite",
            color = DeepHeritageOrange,
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.scale(scale.value)
        )
    }
}
