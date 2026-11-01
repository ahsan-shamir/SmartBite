package com.example.smartbitesample

data class UserEntity(
    val username: String = "",
    val email: String = "",
    val name: String = "",
    val password: String = "",
    val age: Int = 0,
    val gender: String = "",
    val diseases: String = "",
    val profileImageUrl: String = "",
    val address: String = ""
)

data class CartItem(
    val id: String = "",
    val foodId: Int = 0,
    val foodName: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val ingredients: String = "",
    val nutrition: String = "",
    val description: String = "",
    val history: String = "",
    val isHighSugar: Boolean = false,
    val isHighSalt: Boolean = false,
    val isOily: Boolean = false
)

data class OrderEntity(
    val id: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val status: String = "Pending", // Pending, On the way, Delivered
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryAddress: String = ""
)

data class HistoryEntity(
    val id: String = "",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Delivered",
    val deliveryAddress: String = ""
)

data class FavoriteEntity(
    val foodName: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val ingredients: String = "",
    val nutrition: String = "",
    val description: String = "",
    val history: String = "",
    val isHighSugar: Boolean = false,
    val isHighSalt: Boolean = false,
    val isOily: Boolean = false
)

data class NotificationEntity(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
