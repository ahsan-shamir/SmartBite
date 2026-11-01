package com.example.smartbitesample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartbitesample.OrderEntity
import com.example.smartbitesample.data.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OrderViewModel(private val repository: OrderRepository) : ViewModel() {

    val activeOrders: StateFlow<List<OrderEntity>> = repository.getActiveOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.deleteActiveOrder(order.id)
        }
    }
}
