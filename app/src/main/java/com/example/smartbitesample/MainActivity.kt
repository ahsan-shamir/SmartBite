package com.example.smartbitesample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.*
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.BottomAppBar as M2BottomAppBar
import androidx.compose.material.FabPosition as M2FabPosition
import androidx.compose.material.Scaffold as M2Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import com.google.firebase.messaging.FirebaseMessaging
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

import com.example.smartbitesample.data.OrderRepository
import com.example.smartbitesample.data.UserRepository
import com.example.smartbitesample.ui.auth.AuthViewModel
import com.example.smartbitesample.ui.auth.LoginScreen
import com.example.smartbitesample.ui.auth.RegistrationScreen
import com.example.smartbitesample.ui.auth.OtpVerificationScreen
import com.example.smartbitesample.ui.chat.ChatViewModel
import com.example.smartbitesample.ui.home.*
import com.example.smartbitesample.ui.theme.SmartBiteSampleTheme
import com.example.smartbitesample.ui.splash.SplashScreen
import com.example.smartbitesample.ui.components.*
import com.example.smartbitesample.components.SmartRecommendationsScreen
import com.example.smartbitesample.ui.theme.DeepHeritageOrange

class MainActivity : ComponentActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var functions: FirebaseFunctions
    private var homeViewModelRef: HomeViewModel? = null
    private var showDemoDialog = mutableStateOf(false)
    private var lastAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        functions = FirebaseFunctions.getInstance()
        functions.useEmulator("192.168.10.6", 5001)
        
        paymentSheet = PaymentSheet(this) { result ->
            onPaymentResult(result)
        }

        enableEdgeToEdge()
        setContent {
            val systemInDarkTheme = isSystemInDarkTheme()
            val userDarkModePreference = rememberSaveable { mutableStateOf<Boolean?>(null) }
            val darkTheme = userDarkModePreference.value ?: systemInDarkTheme

            SmartBiteSampleTheme(darkTheme = darkTheme) {
                Box {
                    SmartBiteApp(
                        isDarkMode = darkTheme,
                        onThemeToggle = {
                            userDarkModePreference.value = !darkTheme
                        },
                        onTriggerPayment = { amount ->
                            lastAmount = amount
                            fetchPaymentIntent(amount)
                        },
                        onViewModelInit = { vm ->
                            homeViewModelRef = vm
                            handleIntent(intent)
                        }
                    )

                    if (showDemoDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showDemoDialog.value = false },
                            title = { Text("Server is not connected.", fontWeight = FontWeight.Bold) },
                            text = { Text("Server connection failed (Likely not on demo Wi-Fi).\n\nWould you like to proceed with a Simulated Payment for this evaluation?") },
                            confirmButton = {
                                Button(onClick = {
                                    showDemoDialog.value = false
                                    homeViewModelRef?.checkout("Paid")
                                    Toast.makeText(this@MainActivity, "Demo: Payment Successful!", Toast.LENGTH_LONG).show()
                                    homeViewModelRef?.openOrderList()
                                }) {
                                    Text("Simulate Payment")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDemoDialog.value = false }) {
                                    Text("Cancel")
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val title = intent.getStringExtra("notification_title")
            ?: intent.getStringExtra("title")
            ?: intent.getStringExtra("gcm.notification.title")

        val message = intent.getStringExtra("notification_message")
            ?: intent.getStringExtra("message")
            ?: intent.getStringExtra("body")
            ?: intent.getStringExtra("gcm.notification.body")

        if (!title.isNullOrEmpty() || !message.isNullOrEmpty()) {
            val finalTitle = title ?: "SmartBite"
            val finalMessage = message ?: ""
            Log.d("SmartBite_FCM", "MainActivity Captured: $finalTitle")
            homeViewModelRef?.saveNotification(finalTitle, finalMessage)

            intent.removeExtra("notification_title")
            intent.removeExtra("notification_message")
            intent.removeExtra("title")
            intent.removeExtra("message")
            intent.removeExtra("body")
            intent.removeExtra("gcm.notification.title")
            intent.removeExtra("gcm.notification.body")
        }
    }

    private fun fetchPaymentIntent(amount: Double) {
        val finalAmount = if (amount < 50) 50.0 else amount 
        val data = hashMapOf("amount" to (finalAmount * 100).toInt(), "currency" to "pkr")
        functions.getHttpsCallable("createPaymentIntent").call(data).addOnSuccessListener { result ->
            val response = result.data as Map<*, *>
            presentPaymentSheet(response["clientSecret"] as String)
        }.addOnFailureListener { e ->
            Log.e("MainActivity", "Error fetching payment intent", e)
            showDemoDialog.value = true
        }
    }

    private fun presentPaymentSheet(clientSecret: String) {
        val googlePayConfiguration = PaymentSheet.GooglePayConfiguration(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            countryCode = "PK", currencyCode = "PKR",
        )
        paymentSheet.presentWithPaymentIntent(clientSecret, PaymentSheet.Configuration(
            merchantDisplayName = "SmartBite Inc.", googlePay = googlePayConfiguration, allowsDelayedPaymentMethods = true
        ))
    }

    private fun onPaymentResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> Log.d("Stripe", "Canceled")
            is PaymentSheetResult.Failed -> Log.e("Stripe", "Error: ${paymentSheetResult.error}")
            is PaymentSheetResult.Completed -> {
                homeViewModelRef?.checkout("Paid")
                Toast.makeText(this, "Payment Successful! Order placed.", Toast.LENGTH_LONG).show()
                homeViewModelRef?.openOrderList()
            }
        }
    }
}

@Composable
fun SmartBiteApp(
    isDarkMode: Boolean, 
    onThemeToggle: () -> Unit,
    onTriggerPayment: (Double) -> Unit,
    onViewModelInit: (HomeViewModel) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val userRepository = remember { UserRepository() }
    val orderRepository = remember { OrderRepository() }

    val authViewModel: AuthViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = AuthViewModel(userRepository) as T
        }
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(app, orderRepository)
    )
    
    LaunchedEffect(Unit) {
        authViewModel.checkUser(context)
        onViewModelInit(homeViewModel)
    }

    val chatViewModel: ChatViewModel = viewModel()
    val recommendationsViewModel: SmartRecommendationsViewModel = viewModel()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val cartItems by homeViewModel.cartItems.collectAsStateWithLifecycle()
    val history by homeViewModel.history.collectAsStateWithLifecycle()
    val favorites by homeViewModel.favorites.collectAsStateWithLifecycle()
    val notifications by homeViewModel.notifications.collectAsStateWithLifecycle()
    val weatherDesc by homeViewModel.weatherDesc.collectAsStateWithLifecycle()
    val temperature by homeViewModel.temperature.collectAsStateWithLifecycle()
    val formattedTemp by homeViewModel.formattedTemp.collectAsStateWithLifecycle()
    val isLocationDisabled by homeViewModel.isLocationDisabled.collectAsStateWithLifecycle()
    val locationName by homeViewModel.locationName.collectAsStateWithLifecycle()
    val activeTab by homeViewModel.activeTab.collectAsStateWithLifecycle()
    val showChatBot by homeViewModel.showChatBot.collectAsStateWithLifecycle()
    val showOrders by homeViewModel.showOrders.collectAsStateWithLifecycle()
    val showOrderList by homeViewModel.showOrderList.collectAsStateWithLifecycle()
    val showHistory by homeViewModel.showHistory.collectAsStateWithLifecycle()
    val showFavorites by homeViewModel.showFavorites.collectAsStateWithLifecycle()
    val showNotifications by homeViewModel.showNotifications.collectAsStateWithLifecycle()
    val showOrderSuccess by homeViewModel.showOrderSuccess.collectAsStateWithLifecycle()
    val showPaymentGateway by homeViewModel.showPaymentGateway.collectAsStateWithLifecycle()
    val activeOrders by homeViewModel.activeOrders.collectAsStateWithLifecycle()
    val selectedFood by homeViewModel.selectedFood.collectAsStateWithLifecycle()
    val allFoods by homeViewModel.allFoods.collectAsStateWithLifecycle()
    val showRecommendationPopup by homeViewModel.showRecommendationPopup.collectAsStateWithLifecycle()
    val selectedSlot = when {
        showRecommendationPopup || activeTab == "Recommendations" -> 1
        activeTab == "Menu" -> 0
        activeTab == "Cart" -> 2
        activeTab == "Favorites" -> 3
        else -> -1
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { permissions.add(Manifest.permission.POST_NOTIFICATIONS) }
        val missing = permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) { permissionLauncher.launch(missing.toTypedArray()) }
        FirebaseMessaging.getInstance().subscribeToTopic("all")
    }

    LaunchedEffect(currentRoute) { if (currentRoute == "home") homeViewModel.onHomeReached() }

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val drawerWidth = (configuration.screenWidthDp * (if (configuration.screenWidthDp >= 600) 0.5f else 0.8f)).dp

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        val isHomeRoute = currentRoute == "home"
        Box(modifier = Modifier.fillMaxSize()) {
            M2Scaffold(
                scaffoldState = scaffoldState,
                drawerGesturesEnabled = isHomeRoute,
                drawerBackgroundColor = Color.Transparent,
                drawerElevation = 0.dp,
                drawerShape = if (configuration.screenWidthDp >= 600) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                drawerContent = {
                    if (isHomeRoute && authViewModel.currentUser != null) {
                        val user = authViewModel.currentUser!!
                        val textColor = if (isDarkMode) MaterialTheme.colors.onSurface else Color.Black
                        val iconColor = if (isDarkMode) MaterialTheme.colors.primary else Color.Black
                        val selectedBgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                        val drawerBg = if (isDarkMode) Color(0xFF121212) else Color.White
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.width(drawerWidth).fillMaxHeight().background(drawerBg).navigationBarsPadding().clickable(enabled = false) {}) {
                                Box(modifier = Modifier.fillMaxWidth().background(selectedBgColor).statusBarsPadding()) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
                                        if (user.profileImageUrl.isNotEmpty()) {
                                            AsyncImage(model = user.profileImageUrl, contentDescription = "Profile", modifier = Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                        } else {
                                            Box(modifier = Modifier.size(72.dp).background(Color(0xFF6200EE), CircleShape), contentAlignment = Alignment.Center) {
                                                Text(text = user.name.firstOrNull()?.uppercase() ?: "U", color = Color.White, style = MaterialTheme.typography.h4)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(user.name.ifEmpty { user.username }, style = MaterialTheme.typography.h6, color = textColor, fontWeight = FontWeight.Bold)
                                        Text(user.email, style = MaterialTheme.typography.body2, color = textColor.copy(alpha = 0.6f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                @Composable
                                fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, color: Color = textColor, onClick: () -> Unit) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).background(if (isActive) selectedBgColor else Color.Transparent, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = icon, contentDescription = null, tint = if (color == Color.Red) Color.Red else iconColor)
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Text(text = label, color = color, style = MaterialTheme.typography.subtitle1, fontWeight = if(isActive || color == Color.Red) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                                DrawerItem(Icons.Outlined.Person, "Profile", isActive = activeTab == "Profile") { coroutineScope.launch { scaffoldState.drawerState.close() }; navController.navigate("profile") }
                                DrawerItem(Icons.Outlined.AutoAwesome, "Smart Recommendations", isActive = activeTab == "Recommendations") { coroutineScope.launch { scaffoldState.drawerState.close() }; homeViewModel.openRecommendation() }
                                DrawerItem(Icons.Outlined.Notifications, "Notifications", isActive = activeTab == "Notifications") { coroutineScope.launch { scaffoldState.drawerState.close() }; homeViewModel.setActiveTab("Notifications"); homeViewModel.openNotifications() }
                                DrawerItem(Icons.Outlined.ShoppingCart, "Cart", isActive = activeTab == "Cart") { coroutineScope.launch { scaffoldState.drawerState.close() }; homeViewModel.setActiveTab("Cart"); homeViewModel.openCart() }
                                DrawerItem(Icons.Outlined.List, "Order List", isActive = activeTab == "OrderList") { coroutineScope.launch { scaffoldState.drawerState.close() }; homeViewModel.setActiveTab("OrderList"); homeViewModel.openOrderList() }
                                DrawerItem(Icons.Outlined.History, "Order History", isActive = activeTab == "History") { coroutineScope.launch { scaffoldState.drawerState.close() }; homeViewModel.setActiveTab("History"); homeViewModel.openHistory() }
                                DrawerItem(Icons.Outlined.FavoriteBorder, "Favorites", isActive = activeTab == "Favorites") { coroutineScope.launch { scaffoldState.drawerState.close() }; homeViewModel.setActiveTab("Favorites"); homeViewModel.openFavorites() }
                                DrawerItem(Icons.Outlined.SupportAgent, "Contact Us", isActive = currentRoute == "contact_Us") { coroutineScope.launch { scaffoldState.drawerState.close() }; navController.navigate("contact_Us") }
                                Spacer(modifier = Modifier.weight(1f))
                                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = iconColor.copy(alpha = 0.1f))
                                DrawerItem(icon = Icons.AutoMirrored.Outlined.Logout, label = "Logout", isActive = false, color = Color.Red) { coroutineScope.launch { scaffoldState.drawerState.close() }; showLogoutDialog = true }
                                DrawerItem(icon = Icons.Outlined.DeleteForever, label = "Delete Account", isActive = false, color = Color.Red) { coroutineScope.launch { scaffoldState.drawerState.close() }; showDeleteDialog = true }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                },
                bottomBar = {
                    if (isHomeRoute && authViewModel.currentUser != null) {
                        M2BottomAppBar(
                            backgroundColor = if (isDarkMode) com.example.smartbitesample.ui.theme.NavFrameCharcoal else MaterialTheme.colors.primary,
                            cutoutShape = CircleShape, elevation = 12.dp, modifier = Modifier.navigationBarsPadding().height(52.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = if (configuration.screenWidthDp >= 600) 120.dp else 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                @Composable fun NavIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, slot: Int, label: String, onClick: () -> Unit) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clip(CircleShape).clickable { onClick() }.padding(vertical = 4.dp)) {
                                        Icon(icon, null, tint = if (selectedSlot == slot && (showChatBot || selectedFood != null)) (if (isDarkMode) DeepHeritageOrange else Color.White) else (if (isDarkMode) MaterialTheme.colors.primary else Color.White), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        AnimatedVisibility(visible = selectedSlot == slot, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) { Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape)) }
                                    }
                                }
                                NavIcon(Icons.Outlined.Menu, 0, "Menu") { homeViewModel.setActiveTab("Menu"); coroutineScope.launch { scaffoldState.drawerState.open() } }
                                NavIcon(Icons.Outlined.AutoAwesome, 1, "Recs") { homeViewModel.setActiveTab("Recommendations"); homeViewModel.openRecommendation() }
                                Spacer(modifier = Modifier.width(48.dp))
                                NavIcon(Icons.Outlined.ShoppingBag, 2, "Cart") { homeViewModel.setActiveTab("Cart"); homeViewModel.openCart() }
                                NavIcon(Icons.Outlined.FavoriteBorder, 3, "Favs") { homeViewModel.setActiveTab("Favorites"); homeViewModel.openFavorites() }
                            }
                        }
                    }
                },
                floatingActionButton = { if (isHomeRoute) { FloatingActionButton(onClick = { homeViewModel.openChatBot() }, shape = CircleShape, backgroundColor = if (isDarkMode) DeepHeritageOrange else MaterialTheme.colors.secondary, elevation = FloatingActionButtonDefaults.elevation(8.dp), modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SmartToy, "AI Chat", tint = Color.White) } } },
                floatingActionButtonPosition = M2FabPosition.Center, isFloatingActionButtonDocked = true,
                content = { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                var timeout by remember { mutableStateOf(false) }
                                LaunchedEffect(timeout, authViewModel.isInitialized) { if (timeout && authViewModel.isInitialized) { navController.navigate(if (authViewModel.currentUser == null) "login" else "home") { popUpTo("splash") { inclusive = true } } } }
                                SplashScreen(onTimeout = { timeout = true })
                            }
                            composable("login") { val loginContext = LocalContext.current; LoginScreen(onLogin = { e, p -> authViewModel.loginUser(e, p, loginContext) { homeViewModel.resetRecommendationState(); navController.navigate("home") { popUpTo("login") { inclusive = true } } } }, onNavigateToRegister = { navController.navigate("registration") }, error = authViewModel.authError, isLoading = authViewModel.isLoading) }
                            composable("registration") { val regContext = LocalContext.current; RegistrationScreen(onRegister = { newUser -> authViewModel.startRegistration(newUser, regContext) { navController.navigate("otp") } }, error = authViewModel.authError, isLoading = authViewModel.isLoading) }
                            composable("otp") { val otpContext = LocalContext.current; OtpVerificationScreen(email = authViewModel.pendingUser?.email ?: "", onVerify = { otp -> authViewModel.verifyOtpAndRegister(otp, otpContext) { homeViewModel.resetRecommendationState(); navController.navigate("home") { popUpTo("login") { inclusive = true } } } }, onResend = { authViewModel.pendingUser?.let { authViewModel.startRegistration(it, otpContext) {} } }, error = authViewModel.authError, notice = authViewModel.otpDeliveryNotice, isLoading = authViewModel.isLoading) }
                            composable("home") { authViewModel.currentUser?.let { user -> HomeScreen(user = user, viewModel = homeViewModel, weatherDesc = weatherDesc, temperature = temperature, formattedTemp = formattedTemp, isLocationDisabled = isLocationDisabled, locationName = locationName, isDark = isDarkMode, onThemeToggle = onThemeToggle, onFoodClick = { homeViewModel.openFoodDetail(it) }, onFavoriteClick = { homeViewModel.toggleFavorite(it) }, onAddToCart = { homeViewModel.addToCart(it) }, favorites = favorites.map { it.foodName }) } }
                            composable("profile") { authViewModel.currentUser?.let { user -> com.example.smartbitesample.ui.profile.ProfileScreen(user = user, onBack = { navController.popBackStack() }, authViewModel = authViewModel, homeViewModel = homeViewModel) } }
                            composable("contact_Us") { authViewModel.currentUser?.let { user -> ContactUsScreen(user = user, onBack = { navController.popBackStack() }) } ?: LaunchedEffect(Unit) { navController.navigate("login") { popUpTo(0) { inclusive = true } } } }
                        }
                        AnimatedVisibility(visible = showRecommendationPopup, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }), exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })) {
                            val user = authViewModel.currentUser
                            SmartRecommendationsScreen(viewModel = recommendationsViewModel, onDismiss = { homeViewModel.dismissRecommendation() }, isDark = isDarkMode, userDiseases = user?.diseases ?: "", onUpdateDiseases = { d -> user?.let { authViewModel.updateProfile(it.copy(diseases = d)) {} } }, onFoodClick = { homeViewModel.openFoodDetail(it) }, onFavoriteClick = { homeViewModel.toggleFavorite(it) }, onAddToCart = { homeViewModel.addToCart(it) }, favorites = favorites.map { it.foodName }, cartItemCount = cartItems.size, locationName = locationName, weatherDesc = weatherDesc, allFoods = allFoods)
                        }
                        if (authViewModel.isDeleting) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(enabled = false) { }, contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFA500)) } }
                    }
                }
            )
            val showAddedAlert by homeViewModel.showAddedToCartAlert.collectAsStateWithLifecycle()
            if (showAddedAlert) { Popup(alignment = Alignment.BottomCenter, properties = PopupProperties(focusable = false)) { androidx.compose.animation.AnimatedVisibility(visible = showAddedAlert, enter = fadeIn() + slideInVertically(initialOffsetY = { it }), exit = fadeOut() + slideOutVertically(targetOffsetY = { it }), modifier = Modifier.padding(bottom = 110.dp) ) { Surface(color = MaterialTheme.colors.onSurface.copy(alpha = 0.9f), shape = RoundedCornerShape(24.dp), elevation = 12.dp) { Text(text = "Added to cart!", color = MaterialTheme.colors.surface, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp), fontWeight = FontWeight.Bold, fontSize = 14.sp) } } } }
            if (showDeleteDialog) { val deleteContext = LocalContext.current; AlertDialog(onDismissRequest = { if (!authViewModel.isDeleting) showDeleteDialog = false }, shape = RoundedCornerShape(24.dp), title = { Text("Delete Account", fontWeight = FontWeight.Bold) }, text = { Text("Are you sure you want to permanently delete your account?") }, buttons = { Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }; TextButton(onClick = { showDeleteDialog = false; authViewModel.deleteAccount(deleteContext) { navController.navigate("registration") { popUpTo(0) { inclusive = true } } } }) { Text("Delete", color = Color.Red) } } }) }
            if (showLogoutDialog) { val logoutContext = LocalContext.current; AlertDialog(onDismissRequest = { if (!isLoggingOut) showLogoutDialog = false }, shape = RoundedCornerShape(24.dp), title = { Text("Logout", fontWeight = FontWeight.Bold) }, text = { Text("Are you sure you want to logout?") }, buttons = { Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }; TextButton(onClick = { coroutineScope.launch { isLoggingOut = true; authViewModel.logout(logoutContext) { isLoggingOut = false; showLogoutDialog = false; navController.navigate("login") { popUpTo(0) { inclusive = true } } } } }) { Text("Logout") } } }) }
            if (showOrders) { authViewModel.currentUser?.let { user -> CartDialog(cartItems = cartItems, user = user, onDismiss = { homeViewModel.closeCart() }, onCheckout = { a -> homeViewModel.openPaymentGateway(a) }, onDelete = { homeViewModel.removeFromCart(it) }) } }
            if (showPaymentGateway) { PaymentGatewayDialog(totalAmount = cartItems.sumOf { it.price * it.quantity }, onDismiss = { homeViewModel.closePaymentGateway() }, onPaymentSuccess = { m -> homeViewModel.checkout(m); homeViewModel.openOrderList() }, onStripePayment = { onTriggerPayment(cartItems.sumOf { it.price * it.quantity }); homeViewModel.closePaymentGateway() }) }
            if (showOrderList) { ActiveOrdersDialog(orders = activeOrders, onDismiss = { homeViewModel.closeOrderList() }, onCancelOrder = { o -> homeViewModel.cancelOrder(orderId = o.id) }) }
            if (showHistory) { HistoryDialog(history = history, onDismiss = { homeViewModel.closeHistory() }, onClearHistory = { homeViewModel.clearHistory() }) }
            if (showFavorites) { FavoritesDialog(favorites = favorites, onDismiss = { homeViewModel.closeFavorites() }, onDelete = { homeViewModel.deleteFavorite(it) }, onAddToCart = { fav -> val food = allFoods.firstOrNull { it.name == fav.foodName }; if (food != null) homeViewModel.addToCart(food) }) }
            if (showNotifications) { NotificationsDialog(notifications = notifications, onDismiss = { homeViewModel.closeNotifications() }, onClearAll = { homeViewModel.clearNotifications() }) }
            if (showOrderSuccess) { OrderSuccessDialog { homeViewModel.dismissOrderSuccess() } }
            if (showChatBot) { LaunchedEffect(showChatBot) { chatViewModel.updateAppContext(allFoods, locationName, formattedTemp, weatherDesc) }; AIChatBotDialog(chatViewModel = chatViewModel, onDismiss = { homeViewModel.closeChatBot() }) }
            selectedFood?.let { food -> FoodDetailDialog(food = food, onDismiss = { homeViewModel.closeFoodDetail() }, onAddToCart = { q -> homeViewModel.addToCart(food, q); homeViewModel.closeFoodDetail() }) }
        }
    }
}
