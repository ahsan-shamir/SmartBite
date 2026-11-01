package com.example.smartbitesample.ui.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbitesample.UserEntity
import com.example.smartbitesample.data.EmailService
import com.example.smartbitesample.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(private val repository: UserRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val emailService = EmailService.create()

    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isDeleting by mutableStateOf(false)
        private set

    var isInitialized by mutableStateOf(false)
        private set

    var authError by mutableStateOf<String?>(null)
        private set

    var otpDeliveryNotice by mutableStateOf<String?>(null)
        private set

    var generatedOtp by mutableStateOf<String?>(null)
    var pendingUser by mutableStateOf<UserEntity?>(null)

    private fun cacheUsername(context: Context, username: String) {
        context.getSharedPreferences("smart_bite_prefs", Context.MODE_PRIVATE)
            .edit { putString("last_logged_in_username", username) }
        Log.d("AuthViewModel", "Cached username for FCM: $username")
    }

    fun checkUser(context: Context? = null) {
        viewModelScope.launch {
            isLoading = true
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val user = repository.getUser()
                    if (user != null) {
                        currentUser = user
                        context?.let { cacheUsername(it, user.username) }
                        Log.d("AuthViewModel", "User session restored: ${user.username}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Check user failed", e)
            } finally {
                isLoading = false
                isInitialized = true
            }
        }
    }

    fun loginUser(email: String, pass: String, context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            authError = null
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val user = repository.getUser()
                if (user != null) {
                    currentUser = user
                    cacheUsername(context, user.username)
                    onComplete()
                } else {
                    authError = "User profile not found"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login failed", e)
                authError = e.localizedMessage ?: "Login failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun startRegistration(user: UserEntity, context: Context? = null, onOtpSent: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            authError = null
            otpDeliveryNotice = null
            try {
                if (repository.isEmailRegistered(user.email)) {
                    authError = "Email is already registered"
                    return@launch
                }
                if (!repository.isUsernameUnique(user.username)) {
                    authError = "Username is already taken"
                    return@launch
                }

                val otp = (100000..999999).random().toString()
                generatedOtp = otp
                pendingUser = user

                try {
                    emailService.sendOtpEmail(
                        toEmail = user.email,
                        recipientName = user.name.ifBlank { user.username },
                        otp = otp
                    )
                    Log.d("AuthViewModel", "OTP sent to ${user.email}")
                    context?.let {
                        Toast.makeText(it, "Verification code sent to ${user.email}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Gmail SMTP send failed: ${e.message}", e)
                    Log.d("AuthViewModel", "TESTING OTP: $otp")
                    otpDeliveryNotice = "Note: If email delivery was delayed, your OTP code is: $otp"
                    context?.let {
                        Toast.makeText(it, "Could not send email. Test OTP: $otp", Toast.LENGTH_LONG).show()
                    }
                }

                onOtpSent()
            } catch (e: Exception) {
                authError = e.localizedMessage ?: "Registration failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun verifyOtpAndRegister(otp: String, context: Context, onComplete: () -> Unit) {
        if (otp != generatedOtp || pendingUser == null) {
            authError = "Invalid OTP"
            return
        }

        viewModelScope.launch {
            isLoading = true
            authError = null
            val user = pendingUser!!
            try {
                auth.createUserWithEmailAndPassword(user.email, user.password).await()
                repository.insertUser(user)
                currentUser = user
                cacheUsername(context, user.username)
                generatedOtp = null
                pendingUser = null
                otpDeliveryNotice = null
                onComplete()
            } catch (e: Exception) {
                authError = e.localizedMessage ?: "Registration failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun logout(context: Context, onComplete: () -> Unit) {
        context.getSharedPreferences("smart_bite_prefs", Context.MODE_PRIVATE)
            .edit { remove("last_logged_in_username") }
        auth.signOut()
        currentUser = null
        otpDeliveryNotice = null
        onComplete()
    }

    fun updateProfile(updatedUser: UserEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                repository.insertUser(updatedUser)
                currentUser = updatedUser
                onComplete()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Update profile failed", e)
                authError = e.localizedMessage ?: "Update failed"
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteAccount(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            isDeleting = true
            authError = null
            val userCopy = currentUser
            val firebaseUser = auth.currentUser

            if (userCopy != null && firebaseUser != null) {
                try {
                    repository.archiveDeletedUser(userCopy)
                    repository.deleteUser(userCopy.username)
                    try {
                        firebaseUser.delete().await()
                        context.getSharedPreferences("smart_bite_prefs", Context.MODE_PRIVATE)
                            .edit { remove("last_logged_in_username") }
                        currentUser = null
                        isDeleting = false
                        otpDeliveryNotice = null
                        onComplete()
                    } catch (e: Exception) {
                        repository.insertUser(userCopy)
                        isDeleting = false
                        authError = if (e is FirebaseAuthRecentLoginRequiredException ||
                            e.message?.contains("recent-login", ignoreCase = true) == true) {
                            "Security check: Please log out and log in again before deleting your account."
                        } else {
                            e.localizedMessage ?: "Delete failed"
                        }
                    }
                } catch (e: Exception) {
                    isDeleting = false
                    authError = e.localizedMessage ?: "Delete failed"
                }
            } else {
                isDeleting = false
                onComplete()
            }
        }
    }

    fun clearError() {
        authError = null
        otpDeliveryNotice = null
    }
}
