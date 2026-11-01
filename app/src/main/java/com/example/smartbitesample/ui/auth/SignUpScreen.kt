package com.example.smartbitesample.ui.auth

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.smartbitesample.UserEntity

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel
) {
    var showOtpScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    if (!showOtpScreen) {
        RegistrationScreen(
            onRegister = { user ->
                viewModel.startRegistration(user) {
                    showOtpScreen = true
                }
            },
            error = viewModel.authError,
            isLoading = viewModel.isLoading
        )
    } else {
        OtpVerificationScreen(
            email = viewModel.pendingUser?.email ?: "",
            onVerify = { otp ->
                viewModel.verifyOtpAndRegister(otp, context) {
                    onSignUpSuccess()
                }
            },
            onResend = {
                viewModel.pendingUser?.let { user ->
                    viewModel.startRegistration(user) {}
                }
            },
            error = viewModel.authError,
            isLoading = viewModel.isLoading
        )
    }
}
