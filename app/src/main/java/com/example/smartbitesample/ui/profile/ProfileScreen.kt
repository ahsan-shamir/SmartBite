package com.example.smartbitesample.ui.profile

import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.smartbitesample.UserEntity
import com.example.smartbitesample.ui.auth.AuthViewModel
import com.example.smartbitesample.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.example.smartbitesample.ui.components.DiseaseSelectionDialog

@Composable
fun ProfileScreen(
    user: UserEntity,
    onBack: () -> Unit,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel
) {
    BackHandler(onBack = onBack)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Fetch current address if empty or when requested
    val currentLocationName by homeViewModel.locationName.collectAsState()
    val fullAddress by homeViewModel.fullAddress.collectAsState()

    // Editable states
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(user.name) }
    var gender by remember { mutableStateOf(user.gender) }
    var diseases by remember { mutableStateOf(user.diseases) }
    var address by remember { mutableStateOf(user.address) }
    var showDiseaseDialog by remember { mutableStateOf(false) }

    // Always fetch latest location and update address on screen open
    LaunchedEffect(Unit) {
        homeViewModel.fetchLocationAndWeather()
    }

    LaunchedEffect(fullAddress) {
        if (fullAddress.isNotEmpty() && fullAddress != "Detecting..." && fullAddress != "Location Off") {
            address = fullAddress
            // Also update the user entity in background to save it
            if (user.address != fullAddress) {
                authViewModel.updateProfile(user.copy(address = fullAddress)) {}
            }
        }
    }
    
    // Automatically update address if live location is requested manually
    var shouldUpdateFromLive by remember { mutableStateOf(false) }
    LaunchedEffect(shouldUpdateFromLive, fullAddress) {
        if (shouldUpdateFromLive && fullAddress.isNotEmpty() && fullAddress != "Detecting...") {
            address = fullAddress
            shouldUpdateFromLive = false
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = MaterialTheme.colors.onSurface.copy(alpha=0.87f)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colors.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isEditing) {
                            val updatedUser = user.copy(name = name, gender = gender, diseases = diseases, address = address)
                            authViewModel.updateProfile(updatedUser) { isEditing = false }
                        } else {
                            isEditing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = MaterialTheme.colors.primary
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.background,
                elevation = 0.dp
            )
        },
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // Shrink scrollable area when keyboard opens
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar 
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = "Profile Image",
                    modifier = Modifier.size(120.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(120.dp).background(Color(0xFF6200EE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.firstOrNull()?.uppercase() ?: "U", 
                        color = Color.White, 
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (isEditing) {
                        EditableProfileField("Full Name (Max 25 chars)", name, onValueChange = { 
                            if (it.length <= 25) name = it
                        })
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Gender", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(gender, color = MaterialTheme.colors.onSurface)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colors.onSurface)
                                }
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf("Male", "Female", "Other").forEach {
                                    DropdownMenuItem(onClick = { gender = it; expanded = false }) { Text(it) }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Allergies/Diseases", fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = diseases, 
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    IconButton(onClick = { showDiseaseDialog = true }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            )
                            // Clickable overlay
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDiseaseDialog = true }
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
                        Spacer(modifier = Modifier.height(16.dp))
                        EditableProfileField(
                            label = "Address",
                            value = address,
                            onValueChange = { address = it },
                            trailingIcon = {
                                IconButton(onClick = {
                                    shouldUpdateFromLive = true
                                    homeViewModel.fetchLocationAndWeather()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Get Live Address",
                                        tint = MaterialTheme.colors.primary
                                    )
                                }
                            }
                        )
                    } else {
                        ProfileRow("Full Name", user.name)
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha=0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileRow("Username", user.username)
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha=0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileRow("Email", user.email)
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha=0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileRow("Age", user.age.toString())
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha=0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileRow("Gender", user.gender)
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha=0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileRow("Allergies/Diseases", user.diseases.ifEmpty { "None" })
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha=0.1f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileRow("Address", user.address.ifEmpty { address })
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.87f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EditableProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            trailingIcon = trailingIcon
        )
    }
}
