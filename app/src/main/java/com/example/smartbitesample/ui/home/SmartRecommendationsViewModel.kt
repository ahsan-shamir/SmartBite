package com.example.smartbitesample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RecommendationUiState {
    object Loading : RecommendationUiState()
    // Using a timestamp ensures that 'Success' is always a new state when refreshed
    data class Success(val version: Long = System.currentTimeMillis()) : RecommendationUiState()
    data class Error(val message: String) : RecommendationUiState()
}

class SmartRecommendationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Success())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    fun refreshRecommendations() {
        viewModelScope.launch {
            _uiState.value = RecommendationUiState.Loading
            delay(600) // Small delay for visual feedback
            _uiState.value = RecommendationUiState.Success()
        }
    }
}
