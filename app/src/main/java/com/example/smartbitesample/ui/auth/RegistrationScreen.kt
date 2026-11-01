package com.example.smartbitesample.ui.auth

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartbitesample.UserEntity
import com.example.smartbitesample.ui.components.DiseaseSelectionDialog

@Composable
fun RegistrationScreen(
    onRegister: (UserEntity) -> Unit,
    error: String? = null,
    isLoading: Boolean = false
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var diseases by remember { mutableStateOf("") }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showDiseaseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(28.dp),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Join SmartBite", 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.ExtraBold, 
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Create your healthy food profile", fontSize = 14.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (error != null) {
                        Text(
                            text = error, 
                            color = MaterialTheme.colors.error, 
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    OutlinedTextField(
                        value = name, 
                        onValueChange = { if (it.length <= 25) name = it },
                        label = { Text("Full Name (Max 25 chars)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), 
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("Username") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = email, 
                        onValueChange = { 
                            email = it
                            emailError = if (Patterns.EMAIL_ADDRESS.matcher(it).matches()) null else "Invalid email"
                        },
                        label = { Text("Email") }, modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        shape = RoundedCornerShape(12.dp), enabled = !isLoading
                    )
                    if (emailError != null) {
                        Text(text = emailError!!, color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = password, 
                        onValueChange = { 
                            password = it
                            passwordError = when {
                                it.length < 6 -> "Minimum 6 characters required"
                                !it.any { c -> c.isDigit() } -> "Must contain at least one number"
                                !it.any { c -> c.isUpperCase() } -> "Must contain one uppercase letter"
                                else -> null
                            }
                        },
                        label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.60f)
                                )
                            }
                        },
                        isError = passwordError != null,
                        shape = RoundedCornerShape(12.dp), enabled = !isLoading
                    )
                    if (passwordError != null) {
                        Text(text = passwordError!!, color = MaterialTheme.colors.error, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = age, 
                            onValueChange = { 
                                if (it.all { c -> c.isDigit() }) {
                                    val num = it.toIntOrNull()
                                    if (num == null || num <= 150) age = it
                                }
                            },
                            label = { Text("Age") }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp), enabled = !isLoading
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = gender, onValueChange = {},
                                readOnly = true, label = { Text("Gender") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = { 
                                    IconButton(onClick = { if (!isLoading) expanded = true }) { 
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    } 
                                },
                                enabled = !isLoading
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Male", "Female", "Other").forEach {
                                    DropdownMenuItem(onClick = { gender = it; expanded = false }) {
                                        Text(it)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = diseases, 
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Allergies/Diseases (Optional)") }, 
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), 
                            enabled = !isLoading,
                            trailingIcon = {
                                IconButton(onClick = { if (!isLoading) showDiseaseDialog = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                        )
                        // Invisible clickable overlay to trigger dialog
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(enabled = !isLoading) { showDiseaseDialog = true }
                        )
                    }

                    if (showDiseaseDialog) {
                        DiseaseSelectionDialog(
                            selectedDiseases = if (diseases.isBlank()) emptyList() else diseases.split(", "),
                            onDismiss = { showDiseaseDialog = false },
                            onConfirm = { selectedList ->
                                diseases = selectedList.joinToString(", ")
                                showDiseaseDialog = false
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank() && username.isNotBlank() && emailError == null && passwordError == null) {
                                onRegister(
                                    UserEntity(
                                        username = username,
                                        email = email,
                                        name = name,
                                        password = password,
                                        age = age.toIntOrNull() ?: 0,
                                        gender = gender,
                                        diseases = diseases
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp), enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
