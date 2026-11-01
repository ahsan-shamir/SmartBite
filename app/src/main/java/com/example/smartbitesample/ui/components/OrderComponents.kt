package com.example.smartbitesample.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.smartbitesample.CartItem
import com.example.smartbitesample.HistoryEntity
import com.example.smartbitesample.OrderEntity
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusBadge(status: String) {
    val (color, label) = when (status) {
        "Pending" -> Color.Blue to "Pending"
        "On the Way" -> Color(0xFFFF9800) to "On the Way" // Orange
        "Delivered" -> Color(0xFF4CAF50) to "Delivered" // Green
        "Cancelled" -> Color.Red to "Cancelled" // Red for cancelled
        "Paid" -> Color(0xFF9C27B0) to "Paid" // Purple for Stripe payments
        else -> MaterialTheme.colors.primary to status
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color),
        color = Color.White,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun HistoryDialog(
    history: List<HistoryEntity>, 
    onDismiss: () -> Unit, 
    onClearHistory: () -> Unit = {}
) {
    var selectedHistory by remember { mutableStateOf<HistoryEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Order History", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (history.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No past orders", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(history) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedHistory = item }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Order #${item.id.takeLast(5)}", fontWeight = FontWeight.Bold)
                                        val dateStr = DateFormat.getDateTimeInstance().format(Date(item.timestamp))
                                        Text(dateStr, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                    StatusBadge(item.status)
                                }
                                Text("${if (item.status == "Cancelled") "Cancelled" else "Delivered"} at: ${item.deliveryAddress}", fontSize = 11.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    val itemCount = item.items.sumOf { it.quantity }
                                    Text("$itemCount Items", fontSize = 12.sp)
                                    Text("Rs. ${String.format(Locale.getDefault(), "%.2f", item.totalPrice)}", color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = onClearHistory) { Text("Clear All", color = Color.Red) }
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }

    selectedHistory?.let {
        OrderDetailsDialog(
            items = it.items,
            totalPrice = it.totalPrice,
            status = it.status,
            address = it.deliveryAddress,
            onDismiss = { selectedHistory = null }
        )
    }
}

@Composable
fun OrderDetailsDialog(
    items: List<CartItem>,
    totalPrice: Double,
    status: String,
    address: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Order Details", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", fontWeight = FontWeight.SemiBold)
                    StatusBadge(status)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Delivery Address:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                Text(address, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items) { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity}x ${item.foodName}")
                            Text("Rs. ${String.format(Locale.getDefault(), "%.2f", item.price * item.quantity)}")
                        }
                    }
                }
                
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Rs. ${String.format(Locale.getDefault(), "%.2f", totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colors.primary)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun ActiveOrdersDialog(
    orders: List<OrderEntity>,
    onDismiss: () -> Unit,
    onCancelOrder: (OrderEntity) -> Unit
) {
    var orderToCancel by remember { mutableStateOf<OrderEntity?>(null) }
    var selectedOrderDetails by remember { mutableStateOf<OrderEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colors.surface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Active Orders", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (orders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active orders", color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(orders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { selectedOrderDetails = order },
                                shape = RoundedCornerShape(16.dp),
                                elevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Order #${order.id.takeLast(5)}", fontWeight = FontWeight.Bold)
                                        StatusBadge(order.status)
                                    }
                                    Text("Address: ${order.deliveryAddress}", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f), maxLines = 1)
                                    val totalQty = order.items.sumOf { it.quantity }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("$totalQty Items", fontSize = 13.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f))
                                        Text("Total: Rs. ${String.format(Locale.getDefault(), "%.2f", order.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colors.primary)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (order.status == "Pending") {
                                        OutlinedButton(
                                            onClick = { orderToCancel = order },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Cancel Order")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }

    orderToCancel?.let {
        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel Order?") },
            text = { Text("Are you sure you want to cancel this order?") },
            confirmButton = {
                TextButton(onClick = { 
                    onCancelOrder(it)
                    orderToCancel = null 
                }) { Text("Yes, Cancel", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { orderToCancel = null }) { Text("No") }
            }
        )
    }

    selectedOrderDetails?.let {
        OrderDetailsDialog(
            items = it.items,
            totalPrice = it.totalPrice,
            status = it.status,
            address = it.deliveryAddress,
            onDismiss = { selectedOrderDetails = null }
        )
    }
}

@Composable
fun OrderSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Success!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Text("Your order is now pending.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
            }
        }
    }
}
