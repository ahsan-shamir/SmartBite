package com.example.smartbitesample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartbitesample.ui.theme.ChatDialogSurface
import com.example.smartbitesample.ui.chat.ChatViewModel

@Composable
fun AIChatBotDialog(chatViewModel: ChatViewModel, onDismiss: () -> Unit) {
    var chatInput by remember { mutableStateOf("") }
    val chatMessages by chatViewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()

    // Auto-scroll state
    val listState = rememberLazyListState()

    // Auto-scroll ONLY when a new message arrives (chatMessages.size changes)
    val messageCount = chatMessages.size
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            // +1 accounts for the static welcome message at index 0
            val targetIndex = messageCount // items(chatMessages) starts after item{welcome}, so last item index = messageCount
            listState.animateScrollToItem(targetIndex)
        }
    }

    // Also scroll when loading indicator appears (Smarto is typing...)
    LaunchedEffect(isLoading) {
        if (isLoading && messageCount > 0) {
            listState.animateScrollToItem(messageCount + 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val primaryColor = MaterialTheme.colors.primary
        val isLight = MaterialTheme.colors.isLight
        
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.85f)
                .windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom))
                .drawWithContent {
                    drawContent()
                    val layers = 6
                    val maxStrokePx = 28.dp.toPx()
                    for (i in layers downTo 1) {
                        val strokeWidth = (i.toFloat() / layers) * maxStrokePx
                        val alpha = (1f - (i.toFloat() / layers)) * 0.2f
                        // Orange glow in Light Theme, neutral 3D bevel in Dark Theme
                        val shadowColor = if (isLight) primaryColor.copy(alpha = alpha) else Color.White.copy(alpha = alpha * 0.4f)
                        drawRoundRect(
                            color = shadowColor,
                            size = size,
                            cornerRadius = CornerRadius(24.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = if (isLight) MaterialTheme.colors.surface else ChatDialogSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // === LAYER 1: Header — STATIC, no IME/scroll modifiers ===
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SmartToy, null, tint = MaterialTheme.colors.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Smarto", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { chatViewModel.clearChat() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }

                // === LAYER 2: Message List — FLEXIBLE, shrinks when keyboard opens ===
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    reverseLayout = false
                ) {
                    item {
                        ChatBubble("Hi! I'm Smarto. Ask me anything about healthy food or our menu!", false)
                    }
                    items(chatMessages) { msg ->
                        ChatBubble(msg.first, msg.second)
                    }
                    if (isLoading) {
                        item { ChatBubble("Smarto is typing...", false) }
                    }
                }

                // === LAYER 3: Input Row — pins to keyboard ===
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.imePadding()
                ) {
                    val inputScrollState = rememberScrollState()
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Ask about nutrition...") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 100.dp)
                            .verticalScroll(inputScrollState)
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter) {
                                    if (event.isShiftPressed) {
                                        false // Let Shift+Enter insert a newline
                                    } else {
                                        if (event.type == KeyEventType.KeyUp) {
                                            if (chatInput.isNotBlank() && !isLoading) {
                                                chatViewModel.sendMessage(chatInput)
                                                chatInput = ""
                                            }
                                        }
                                        true // Consume Enter key to prevent newline
                                    }
                                } else {
                                    false
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank() && !isLoading) {
                                chatViewModel.sendMessage(chatInput)
                                chatInput = ""
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.size(38.dp).background(MaterialTheme.colors.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A basic markdown parser to convert **text** to bold FontWeight.
 */
fun parseMarkdownText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(parts[i])
                }
            } else {
                append(parts[i])
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    val clipboardManager = LocalClipboardManager.current

    // ── Copy state: toggles icon to a tick for 2 seconds after clicking ──────
    var isCopied by remember { mutableStateOf(false) }
    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            )
        ) {
            Text(
                text = if (isUser) AnnotatedString(text) else parseMarkdownText(text),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = if (isUser) Color.White else MaterialTheme.colors.onSurface,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily.SansSerif
            )
        }

        // ── Copy button — shown only on Smarto (AI) responses ─────────────────
        if (!isUser) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(text))
                    isCopied = true
                },
                modifier = Modifier
                    .padding(start = 4.dp, top = 1.dp)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (isCopied) "Copied" else "Copy response",
                    tint = if (isCopied)
                        MaterialTheme.colors.primary
                    else
                        MaterialTheme.colors.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
