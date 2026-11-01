package com.example.smartbitesample.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartbitesample.CartItem
import com.example.smartbitesample.UserEntity
import java.util.Locale

@Composable
fun CartDialog(
    cartItems: List<CartItem>,
    user: UserEntity,
    onDismiss: () -> Unit,
    onCheckout: (String) -> Unit,
    onDelete: (CartItem) -> Unit
) {
    var deliveryAddress by remember { mutableStateOf(user.address) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
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
                    Text("Your Cart", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colors.onSurface)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (cartItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Cart is empty", color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        item {
                            Text("Delivery Address", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                            OutlinedTextField(
                                value = deliveryAddress,
                                onValueChange = { deliveryAddress = it },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("Enter delivery address") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                        }
                        items(cartItems) { item ->
                            val totalItemPrice = item.price * item.quantity
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colors.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item.foodName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(
                                            text = "Qty: ${item.quantity} × Rs. ${item.price}", 
                                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f), 
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Total: Rs. $totalItemPrice", 
                                            color = MaterialTheme.colors.primary, 
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onCheckout(deliveryAddress) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = cartItems.isNotEmpty() && deliveryAddress.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    val totalPrice = cartItems.sumOf { it.price * it.quantity }
                    Text("Proceed to Payment (Rs. ${String.format(Locale.getDefault(), "%.2f", totalPrice)})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PaymentGatewayDialog(
    totalAmount: Double,
    onDismiss: () -> Unit,
    onPaymentSuccess: (String) -> Unit,
    onStripePayment: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Online") } // Online, COD
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = if (isProcessing) ({}) else onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Payment Method", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Payable: Rs. ${String.format(Locale.getDefault(), "%.2f", totalAmount)}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text("Select Payment Method", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethodTab("Online Payment", selectedMethod == "Online", Modifier.weight(1f)) { selectedMethod = "Online" }
                    PaymentMethodTab("COD", selectedMethod == "COD", Modifier.weight(1f)) { selectedMethod = "COD" }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                AnimatedContent(targetState = selectedMethod) { method ->
                    when (method) {
                        "Online" -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                Text("Pay securely with Debit/Credit card.", textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                            }
                        }
                        "COD" -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                Text("Pay with cash when your food arrives.", textAlign = TextAlign.Center, color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (selectedMethod == "Online") {
                            onDismiss()
                            onStripePayment()
                        } else {
                            isProcessing = true
                            onPaymentSuccess(selectedMethod)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (selectedMethod == "COD") "Confirm Order" else "Proceed to Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
                
                if (!isProcessing) {
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Cancel", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodTab(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.05f))
            .clickable { onClick() }
            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colors.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) Color.White else MaterialTheme.colors.onSurface, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}
