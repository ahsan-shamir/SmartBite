package com.example.smartbitesample.ui.home

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbitesample.*
import com.example.smartbitesample.data.OrderRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeViewModel(
    application: Application,
    private val repository: OrderRepository
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val weatherService = WeatherService.create()

    private val _allFoods = MutableStateFlow<List<FoodItem>>(emptyList())
    val allFoods: StateFlow<List<FoodItem>> = _allFoods.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredFoods: StateFlow<List<FoodItem>> = combine(_allFoods, _searchQuery, _selectedCategory) { foods, query, category ->
        val sourceList = if (foods.isEmpty()) sampleFoods else foods
        sourceList.filter { food ->
            (category == "All" || food.category == category) && food.name.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = listOf("All", "Biryani & Rice", "Curries & Karahi", "BBQ & Grills", "Street Food", "Desserts", "Drinks")

    val cartItems: StateFlow<List<CartItem>> = repository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeOrders: StateFlow<List<OrderEntity>> = repository.getActiveOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddedToCartAlert = MutableStateFlow(false)
    val showAddedToCartAlert: StateFlow<Boolean> = _showAddedToCartAlert.asStateFlow()

    // Payment Gateway
    private val _showPaymentGateway = MutableStateFlow(false)
    val showPaymentGateway: StateFlow<Boolean> = _showPaymentGateway.asStateFlow()

    private val _pendingAddress = MutableStateFlow("")
    val pendingAddress: StateFlow<String> = _pendingAddress.asStateFlow()

    fun openPaymentGateway(address: String) {
        _pendingAddress.value = address
        _showOrders.value = false
        _showPaymentGateway.value = true
    }

    fun setPendingAddress(address: String) { _pendingAddress.value = address }
    fun closePaymentGateway() { _showPaymentGateway.value = false }

    // User Profile
    private val userRepository = com.example.smartbitesample.data.UserRepository()
    private val _userProfile = MutableStateFlow<UserEntity?>(null)
    val userProfile: StateFlow<UserEntity?> = _userProfile.asStateFlow()

    val userDiseases: StateFlow<String> = _userProfile.map { it?.diseases ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun fetchUserProfile() {
        viewModelScope.launch { _userProfile.value = userRepository.getUser() }
    }

    private val _recentlyShownIds = MutableStateFlow<List<String>>(emptyList())
    val recentlyShownIds: StateFlow<List<String>> = _recentlyShownIds.asStateFlow()

    fun recordShownItems(ids: List<String>) {
        _recentlyShownIds.value = (_recentlyShownIds.value + ids).takeLast(18)
    }

    init {
        viewModelScope.launch {
            if (_allFoods.value.isEmpty()) _allFoods.value = sampleFoods

            repository.getFoods().collect { cloudFoods ->
                if (cloudFoods.isEmpty()) {
                    repository.syncFoods(sampleFoods)
                } else {
                    val missingInCloud = sampleFoods.filter { local ->
                        cloudFoods.none { it.id == local.id }
                    }
                    if (missingInCloud.isNotEmpty()) repository.syncFoods(missingInCloud)
                }
                _allFoods.value = cloudFoods
            }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onCategorySelected(category: String) { _selectedCategory.value = category }

    fun addToCart(food: FoodItem, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(CartItem(
                foodId = food.id, foodName = food.name, imageUrl = food.imageUrl,
                price = food.price, quantity = quantity, ingredients = food.ingredients,
                nutrition = food.nutrition, description = food.description,
                history = food.history, isHighSugar = food.isHighSugar,
                isHighSalt = food.isHighSalt, isOily = food.isOily
            ))
            _showAddedToCartAlert.value = true
            delay(2000)
            _showAddedToCartAlert.value = false
        }
    }

    fun removeFromCart(item: CartItem) { viewModelScope.launch { repository.removeFromCart(item) } }

    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch {
            val existing = favorites.value.firstOrNull { it.foodName == food.name }
            if (existing != null) repository.deleteFavorite(existing)
            else repository.insertFavorite(FavoriteEntity(
                foodName = food.name, price = food.price, imageUrl = food.imageUrl,
                ingredients = food.ingredients, nutrition = food.nutrition,
                description = food.description, history = food.history,
                isHighSugar = food.isHighSugar, isHighSalt = food.isHighSalt, isOily = food.isOily
            ))
        }
    }

    fun deleteFavorite(favorite: FavoriteEntity) { viewModelScope.launch { repository.deleteFavorite(favorite) } }
    fun clearHistory() { viewModelScope.launch { repository.clearHistory() } }
    fun cancelOrder(orderId: String) { viewModelScope.launch { repository.deleteActiveOrder(orderId) } }

    fun saveNotification(title: String, message: String) {
        viewModelScope.launch { repository.saveNotification(NotificationEntity(title = title, message = message)) }
    }

    fun clearNotifications() { viewModelScope.launch { repository.clearNotifications() } }

    fun checkout(paymentMethod: String) {
        viewModelScope.launch {
            val items = cartItems.value
            val address = _pendingAddress.value
            if (items.isEmpty() || address.isBlank()) return@launch
            repository.placeOrder(OrderEntity(
                items = items,
                totalPrice = items.sumOf { it.price * it.quantity },
                status = if (paymentMethod == "Paid") "Paid" else "Pending",
                timestamp = System.currentTimeMillis(),
                deliveryAddress = address
            ))
            repository.clearCart()
            _showPaymentGateway.value = false
            _showOrderSuccess.value = true
        }
    }

    private val _activeTab = MutableStateFlow("Menu")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()
    fun setActiveTab(tab: String) { _activeTab.value = tab }

    private val _showChatBot = MutableStateFlow(false)
    val showChatBot: StateFlow<Boolean> = _showChatBot.asStateFlow()
    fun openChatBot() { _showChatBot.value = true }
    fun closeChatBot() { _showChatBot.value = false }

    private val _showOrders = MutableStateFlow(false)
    val showOrders: StateFlow<Boolean> = _showOrders.asStateFlow()
    fun openCart() { _showOrders.value = true }
    fun closeCart() { _showOrders.value = false }

    private val _showOrderList = MutableStateFlow(false)
    val showOrderList: StateFlow<Boolean> = _showOrderList.asStateFlow()
    fun openOrderList() { _showOrderList.value = true }
    fun closeOrderList() { _showOrderList.value = false }

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()
    fun openHistory() { _showHistory.value = true }
    fun closeHistory() { _showHistory.value = false }

    private val _showFavorites = MutableStateFlow(false)
    val showFavorites: StateFlow<Boolean> = _showFavorites.asStateFlow()
    fun openFavorites() { _showFavorites.value = true }
    fun closeFavorites() { _showFavorites.value = false }

    private val _showNotifications = MutableStateFlow(false)
    val showNotifications: StateFlow<Boolean> = _showNotifications.asStateFlow()
    fun openNotifications() { _showNotifications.value = true }
    fun closeNotifications() { _showNotifications.value = false }

    private val _showOrderSuccess = MutableStateFlow(false)
    val showOrderSuccess: StateFlow<Boolean> = _showOrderSuccess.asStateFlow()
    fun dismissOrderSuccess() { _showOrderSuccess.value = false }

    private val _selectedFoodId = MutableStateFlow<Int?>(null)
    val selectedFood: StateFlow<FoodItem?> = combine(_allFoods, _selectedFoodId) { foods, id ->
        foods.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun openFoodDetail(food: FoodItem) { _selectedFoodId.value = food.id }
    fun closeFoodDetail() { _selectedFoodId.value = null }

    private val _showRecommendationPopup = MutableStateFlow(false)
    val showRecommendationPopup: StateFlow<Boolean> = _showRecommendationPopup.asStateFlow()
    private var hasShownRecommendation = false

    fun onHomeReached() {
        fetchUserProfile()
        if (!hasShownRecommendation) { _showRecommendationPopup.value = true; hasShownRecommendation = true }
        fetchLocationAndWeather()
    }

    fun resetRecommendationState() { hasShownRecommendation = false }
    fun openRecommendation() { _showRecommendationPopup.value = true }
    fun dismissRecommendation() { _showRecommendationPopup.value = false }
    fun acceptRecommendation() {
        _showRecommendationPopup.value = false
        val food = _allFoods.value.firstOrNull { it.name == "Sindhi Biryani" }
        if (food != null) addToCart(food)
    }

    private val _weatherDesc = MutableStateFlow("Fetching...")
    val weatherDesc: StateFlow<String> = _weatherDesc.asStateFlow()
    private val _temperature = MutableStateFlow(0.0)
    val temperature: StateFlow<Double> = _temperature.asStateFlow()
    private val _formattedTemp = MutableStateFlow("Fetching...")
    val formattedTemp: StateFlow<String> = _formattedTemp.asStateFlow()
    private val _isLocationDisabled = MutableStateFlow(false)
    val isLocationDisabled: StateFlow<Boolean> = _isLocationDisabled.asStateFlow()
    private val _locationName = MutableStateFlow("Detecting...")
    val locationName: StateFlow<String> = _locationName.asStateFlow()
    private val _fullAddress = MutableStateFlow("")
    val fullAddress: StateFlow<String> = _fullAddress.asStateFlow()

    fun fetchLocationAndWeather() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) { _locationName.value = "Location Off"; _isLocationDisabled.value = true; return }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location == null) { _locationName.value = "Location Off"; _isLocationDisabled.value = true; return@addOnSuccessListener }
                _isLocationDisabled.value = false
                viewModelScope.launch {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = withContext(Dispatchers.IO) { geocoder.getFromLocation(location.latitude, location.longitude, 1) }
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            _fullAddress.value = addr.getAddressLine(0) ?: ""
                            _locationName.value = addr.subLocality ?: addr.locality ?: addr.adminArea ?: "Local Area"
                        }
                        val response = weatherService.getWeather(location.latitude, location.longitude)
                        _temperature.value = response.current_weather.temperature
                        _formattedTemp.value = "${response.current_weather.temperature.toInt()}°C"

                        val rawState = when (response.current_weather.weathercode) {
                            0           -> "Sunny"
                            1           -> "Mostly Sunny"
                            2           -> "Partly Cloudy"
                            3           -> "Mostly Cloudy"
                            45, 48      -> "Fog"
                            51, 53, 55  -> "Cloudy"
                            56, 57      -> "Freezing Drizzle"
                            61          -> "Light Rain"
                            63          -> "Moderate Rain"
                            65          -> "Heavy Rain"
                            66, 67      -> "Freezing Rain"
                            71, 73, 75  -> "Snow"
                            77          -> "Snow Grains"
                            80, 81, 82  -> "Rain Showers"
                            85, 86      -> "Snow Showers"
                            95, 96, 99  -> "Thunderstorm"
                            else        -> "Cloudy"
                        }

                        val calendar = java.util.Calendar.getInstance()
                        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                        _weatherDesc.value = if (rawState == "Sunny" && (hour >= 18 || hour < 7)) "Clear" else rawState
                    } catch (e: Exception) {
                        _weatherDesc.value = "Error"
                        _formattedTemp.value = "Error"
                    }
                }
            }
    }
}
