package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.ShoppingItem
import com.example.foodexpiryapp.domain.model.WeeklyStats
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.domain.usecase.DeleteShoppingItemUseCase
import com.example.foodexpiryapp.domain.usecase.GetAllShoppingItemsUseCase
import com.example.foodexpiryapp.domain.usecase.ToggleShoppingItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val getAllShoppingItems: GetAllShoppingItemsUseCase,
    private val toggleShoppingItem: ToggleShoppingItemUseCase,
    private val deleteShoppingItem: DeleteShoppingItemUseCase
) : ViewModel() {

    val shoppingItems: StateFlow<List<ShoppingItem>> = getAllShoppingItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val weeklyStats: StateFlow<WeeklyStats> = analyticsRepository.getWeeklyStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WeeklyStats()
        )

    fun onToggleItem(item: ShoppingItem) {
        viewModelScope.launch {
            toggleShoppingItem(item)
        }
    }

    fun onDeleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            deleteShoppingItem(item)
        }
    }
}
