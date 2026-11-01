package com.example.smartbitesample.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbitesample.UserEntity
import com.example.smartbitesample.ui.theme.DeepHeritageOrange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun ContactUsScreen(user: UserEntity, onBack: () -> Unit) {
    val isDark = !MaterialTheme.colors.isLight
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    // Theme tokens synced with MaterialTheme
    // Synchronized with design tokens
    val screenBgColor = MaterialTheme.colors.background
    val headerTextColor = MaterialTheme.colors.onBackground
    val cardBgColor = MaterialTheme.colors.surface
    val backArrowTint = DeepHeritageOrange 
    val infoTextColor = MaterialTheme.colors.onSurface
    val primaryOrange = DeepHeritageOrange

    Scaffold(
        topBar = {
            // FIX: Using topBar slot makes the header sticky
            Column(modifier = Modifier.background(screenBgColor).statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = backArrowTint
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Contact Us",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    )
                }
                Divider(color = headerTextColor.copy(alpha = 0.05f), thickness = 1.dp)
            }
        },
        backgroundColor = screenBgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(screenBgColor)
                .imePadding() // IME-aware padding BEFORE scroll so the container shrinks for keyboard
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Get in Touch",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) primaryOrange else headerTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = cardBgColor,
                elevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.Start) {
                        ContactDetailItem(Icons.Default.Phone, "+92 347 0515371", infoTextColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        ContactDetailItem(Icons.Default.Phone, "+92 325 5684557", infoTextColor)
                        Spacer(modifier = Modifier.height(24.dp))
                        ContactDetailItem(Icons.Default.Email, "get.smartbite@gmail.com", infoTextColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Send us a Message",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = if (isDark) primaryOrange else headerTextColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message/Query", color = headerTextColor.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(16.dp),
                maxLines = 8,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryOrange,
                    unfocusedBorderColor = headerTextColor.copy(alpha = 0.12f),
                    backgroundColor = cardBgColor,
                    textColor = infoTextColor
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (message.isNotBlank()) {
                        scope.launch {
                            try {
                                val messageData = hashMapOf(
                                    "message" to message,
                                    "timestamp" to Date(),
                                    "senderEmail" to user.email,
                                    "senderName" to user.name
                                )
                                firestore.collection("users")
                                    .document(user.username)
                                    .collection("User-Messages")
                                    .add(messageData)
                                
                                message = ""
                                Toast.makeText(context, "Message sent successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = DeepHeritageOrange,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Text("SEND MESSAGE", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ContactDetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, textColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepHeritageOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
