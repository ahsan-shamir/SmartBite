package com.example.smartbitesample.data

import android.util.Log
import com.example.smartbitesample.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getUser(): UserEntity? {
        val email = auth.currentUser?.email?.lowercase() ?: return null
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            val user = query.documents.firstOrNull()?.toObject(UserEntity::class.java)
            Log.d("UserRepository", "getUser successfully fetched: ${user?.username}")
            user
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user profile", e)
            null
        }
    }

    suspend fun insertUser(user: UserEntity) {
        val normalizedUser = user.copy(
            email = user.email.lowercase().trim(),
            username = user.username.trim()
        )
        firestore.collection("users").document(normalizedUser.username).set(normalizedUser).await()
    }
    
    suspend fun isUsernameUnique(username: String): Boolean {
        return try {
            val snapshot = firestore.collection("users").document(username.trim()).get().await()
            val exists = snapshot.exists()
            Log.d("UserRepository", "Checking username: ${username.trim()}, exists: $exists")
            !exists
        } catch (e: Exception) {
            Log.e("UserRepository", "isUsernameUnique error", e)
            throw e
        }
    }

    suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            val normalizedEmail = email.lowercase().trim()
            val query = firestore.collection("users")
                .whereEqualTo("email", normalizedEmail)
                .get()
                .await()
            val registered = !query.isEmpty
            Log.d("UserRepository", "Checking email: $normalizedEmail, registered: $registered")
            registered
        } catch (e: Exception) {
            Log.e("UserRepository", "isEmailRegistered error", e)
            throw e
        }
    }

    suspend fun deleteUserByEmail(email: String) {
        try {
            val query = firestore.collection("users")
                .whereEqualTo("email", email.lowercase().trim())
                .get()
                .await()
            for (document in query.documents) {
                document.reference.delete().await()
                Log.d("UserRepository", "Deleted Firestore user document: ${document.id}")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user document by email", e)
            throw e
        }
    }

    suspend fun archiveDeletedUser(user: UserEntity) {
        try {
            val archiveData = hashMapOf(
                "username" to user.username,
                "email" to user.email,
                "name" to user.name,
                "password" to user.password,
                "age" to user.age,
                "gender" to user.gender,
                "diseases" to user.diseases,
                "profileImageUrl" to user.profileImageUrl,
                "address" to user.address,
                "deletionTimestamp" to System.currentTimeMillis()
            )
            firestore.collection("deleted_accounts").document(user.username.ifBlank { user.email.replace(".", "_") }).set(archiveData).await()
            Log.d("UserRepository", "User archived in deleted_accounts: ${user.username}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error archiving deleted user", e)
            throw e
        }
    }
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun deleteUser(username: String) {
        try {
            firestore.collection("users").document(username.trim()).delete().await()
            Log.d("UserRepository", "User document $username deleted successfully")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user document", e)
            throw e
        }
    }
}
