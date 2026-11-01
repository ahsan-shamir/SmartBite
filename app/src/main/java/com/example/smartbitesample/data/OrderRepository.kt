package com.example.smartbitesample.data

import android.util.Log
import com.example.smartbitesample.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun getUsernameId(): String {
        var user = auth.currentUser
        var retries = 0
        while (user == null && retries < 15) {
            delay(200)
            user = auth.currentUser
            retries++
        }

        val email = user?.email?.lowercase()?.trim() ?: run {
            Log.e("OrderRepository", "No authenticated user email found")
            return ""
        }

        return try {
            val query = firestore.collection("users").whereEqualTo("email", email).get().await()
            val document = query.documents.firstOrNull()
            document?.id ?: document?.getString("username") ?: ""
        } catch (e: Exception) {
            Log.e("OrderRepository", "Firestore mapping error", e)
            ""
        }
    }

    private fun getUsernameFlow(): Flow<String> = callbackFlow {
        var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            firestoreListener?.remove()
            val email = firebaseAuth.currentUser?.email?.lowercase()?.trim()

            if (email != null) {
                firestoreListener = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("OrderRepository", "Flow snapshot error", error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.firstOrNull()?.id ?: "")
                    }
            } else {
                trySend("")
            }
        }

        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListener?.remove()
        }
    }

    fun getFoods(): Flow<List<FoodItem>> {
        return firestore.collection("foods")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(FoodItem::class.java) }
    }

    suspend fun syncFoods(foods: List<FoodItem>) {
        try {
            val batch = firestore.batch()
            foods.forEach { food ->
                val ref = firestore.collection("foods").document(food.id.toString())
                val foodMap = mutableMapOf<String, Any>(
                    "id" to food.id,
                    "name" to food.name,
                    "category" to food.category,
                    "description" to food.description,
                    "ingredients" to food.ingredients,
                    "nutrition" to food.nutrition,
                    "history" to food.history,
                    "price" to food.price,
                    "imageUrl" to food.imageUrl,
                    "isHighSugar" to food.isHighSugar,
                    "isHighSalt" to food.isHighSalt,
                    "isOily" to food.isOily,
                    "containsMeat" to food.containsMeat
                )
                batch.set(ref, foodMap, SetOptions.merge())
            }
            batch.commit().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error syncing foods", e) }
    }

    suspend fun updateMerchantEarnings(amount: Double) {
        try {
            val ref = firestore.collection("admin").document("earnings")
            ref.set(mapOf("totalBalance" to FieldValue.increment(amount)), SetOptions.merge()).await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error updating earnings", e) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCartItems(): Flow<List<CartItem>> {
        return getUsernameFlow().flatMapLatest { username ->
            if (username.isEmpty()) flowOf(emptyList())
            else firestore.collection("users").document(username)
                .collection("cart")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(CartItem::class.java) }
        }
    }

    suspend fun addToCart(item: CartItem) {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val cartRef = firestore.collection("users").document(username).collection("cart")
            val existingQuery = cartRef.whereEqualTo("foodName", item.foodName).get().await()
            if (existingQuery.isEmpty) {
                val ref = cartRef.document()
                ref.set(item.copy(id = ref.id)).await()
            } else {
                val doc = existingQuery.documents.first()
                val currentQuantity = doc.getLong("quantity")?.toInt() ?: 1
                doc.reference.update("quantity", currentQuantity + item.quantity).await()
            }
        } catch (e: Exception) { Log.e("OrderRepository", "Error adding to cart", e) }
    }

    suspend fun removeFromCart(item: CartItem) {
        val username = getUsernameId()
        if (username.isEmpty() || item.id.isEmpty()) return
        try {
            firestore.collection("users").document(username)
                .collection("cart").document(item.id).delete().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error removing from cart", e) }
    }

    suspend fun clearCart() {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val snapshot = firestore.collection("users").document(username).collection("cart").get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) batch.delete(doc.reference)
            batch.commit().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error clearing cart", e) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActiveOrders(): Flow<List<OrderEntity>> {
        return getUsernameFlow().flatMapLatest { username ->
            if (username.isEmpty()) flowOf(emptyList())
            else firestore.collection("users").document(username)
                .collection("active_orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    val orders = snapshot.toObjects(OrderEntity::class.java)
                    orders.forEach { order ->
                        if (order.status == "Delivered") scope.launch { moveOrderToHistory(username, order) }
                    }
                    orders.filter { it.status != "Delivered" }
                }
        }
    }

    private suspend fun moveOrderToHistory(username: String, order: OrderEntity) {
        try {
            val historyItem = HistoryEntity(
                items = order.items,
                totalPrice = order.totalPrice,
                timestamp = System.currentTimeMillis(),
                status = order.status,
                deliveryAddress = order.deliveryAddress
            )
            val batch = firestore.batch()
            val historyRef = firestore.collection("users").document(username).collection("history").document()
            batch.set(historyRef, historyItem.copy(id = historyRef.id))
            batch.delete(firestore.collection("users").document(username).collection("active_orders").document(order.id))
            batch.commit().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error moving order to history", e) }
    }

    suspend fun placeOrder(order: OrderEntity) {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val ref = firestore.collection("users").document(username).collection("active_orders").document()
            ref.set(order.copy(id = ref.id)).await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error placing order", e) }
    }

    suspend fun deleteActiveOrder(orderId: String) {
        val username = getUsernameId()
        if (username.isEmpty() || orderId.isEmpty()) return
        try {
            val docRef = firestore.collection("users").document(username)
                .collection("active_orders").document(orderId)
            val order = docRef.get().await().toObject(OrderEntity::class.java)
            if (order != null) moveOrderToHistory(username, order.copy(status = "Cancelled"))
            else docRef.delete().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error deleting active order", e) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getHistory(): Flow<List<HistoryEntity>> {
        return getUsernameFlow().flatMapLatest { username ->
            if (username.isEmpty()) flowOf(emptyList())
            else firestore.collection("users").document(username)
                .collection("history")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(HistoryEntity::class.java) }
        }
    }

    suspend fun insertHistory(history: HistoryEntity) {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val ref = firestore.collection("users").document(username).collection("history").document()
            ref.set(history.copy(id = ref.id)).await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error inserting history", e) }
    }

    suspend fun clearHistory() {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val snapshot = firestore.collection("users").document(username).collection("history").get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) batch.delete(doc.reference)
            batch.commit().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error clearing history", e) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFavorites(): Flow<List<FavoriteEntity>> {
        return getUsernameFlow().flatMapLatest { username ->
            if (username.isEmpty()) flowOf(emptyList())
            else firestore.collection("users").document(username)
                .collection("favorites")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FavoriteEntity::class.java) }
        }
    }

    suspend fun insertFavorite(favorite: FavoriteEntity) {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            firestore.collection("users").document(username)
                .collection("favorites").document(favorite.foodName).set(favorite).await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error inserting favorite", e) }
    }

    suspend fun deleteFavorite(favorite: FavoriteEntity) {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            firestore.collection("users").document(username)
                .collection("favorites").document(favorite.foodName).delete().await()
        } catch (e: Exception) { Log.e("OrderRepository", "Error deleting favorite", e) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNotifications(): Flow<List<NotificationEntity>> {
        return getUsernameFlow().flatMapLatest { username ->
            if (username.isEmpty()) flowOf(emptyList())
            else firestore.collection("users").document(username)
                .collection("notifications")
                .snapshots()
                .map { snapshot ->
                    try {
                        snapshot.toObjects(NotificationEntity::class.java)
                            .sortedByDescending { it.timestamp }
                    } catch (e: Exception) {
                        Log.e("OrderRepository", "Error mapping notifications", e)
                        emptyList()
                    }
                }
        }
    }

    suspend fun saveNotification(notification: NotificationEntity) {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val ref = firestore.collection("users").document(username).collection("notifications").document()
            val finalNotif = notification.copy(
                id = ref.id,
                timestamp = if (notification.timestamp <= 0L) System.currentTimeMillis() else notification.timestamp
            )
            ref.set(finalNotif).await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error writing notification to Firestore", e)
        }
    }

    suspend fun clearNotifications() {
        val username = getUsernameId()
        if (username.isEmpty()) return
        try {
            val snapshot = firestore.collection("users").document(username).collection("notifications").get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) batch.delete(doc.reference)
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("OrderRepository", "Error clearing notifications", e)
        }
    }
}
