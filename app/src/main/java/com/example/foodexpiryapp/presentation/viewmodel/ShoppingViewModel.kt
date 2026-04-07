package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.data.local.dao.ShoppingTemplateDao
import com.example.foodexpiryapp.data.local.database.ShoppingTemplateEntity
import com.example.foodexpiryapp.domain.model.ShoppingItem
import com.example.foodexpiryapp.domain.model.ShoppingTemplate
import com.example.foodexpiryapp.domain.model.WeeklyStats
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val getAllShoppingItems: GetAllShoppingItemsUseCase,
    private val toggleShoppingItem: ToggleShoppingItemUseCase,
    private val deleteShoppingItem: DeleteShoppingItemUseCase,
    private val addShoppingItem: AddShoppingItemUseCase,
    private val clearCompletedItems: ClearCompletedShoppingItemsUseCase,
    private val getAllTemplates: GetAllShoppingTemplatesUseCase,
    private val applyTemplate: ApplyShoppingTemplateUseCase,
    private val getInventoryItemNames: GetInventoryItemNamesUseCase,
    private val shoppingTemplateDao: ShoppingTemplateDao
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

    val templates: StateFlow<List<ShoppingTemplate>> = getAllTemplates()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val inventoryItemNames: StateFlow<Set<String>> = getInventoryItemNames()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        seedTemplatesIfNeeded()
    }

    private fun seedTemplatesIfNeeded() {
        viewModelScope.launch {
            val count = shoppingTemplateDao.getTemplateCount()
            if (count == 0) {
                val builtInTemplates = listOf(
                    ShoppingTemplateEntity(
                        name = "Weekly Restock",
                        description = "Your weekly pantry essentials",
                        itemNames = """["Milk","Eggs","Bread","Butter","Chicken breast","Rice","Olive oil","Onions","Garlic","Tomatoes"]"""
                    ),
                    ShoppingTemplateEntity(
                        name = "Breakfast Essentials",
                        description = "Start every morning right",
                        itemNames = """["Eggs","Bread","Butter","Milk","Cereal","Bananas","Yogurt","Orange juice"]"""
                    ),
                    ShoppingTemplateEntity(
                        name = "Fresh Produce",
                        description = "Fresh fruits & vegetables",
                        itemNames = """["Spinach","Tomatoes","Avocados","Bell peppers","Carrots","Cucumbers","Berries","Lemons"]"""
                    )
                )
                shoppingTemplateDao.insertTemplates(builtInTemplates)
            }
        }
    }

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

    fun onAddItem(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            addShoppingItem(name.trim())
        }
    }

    fun applyTemplate(template: ShoppingTemplate) {
        viewModelScope.launch {
            applyTemplate(template)
        }
    }

    fun onClearCompleted() {
        viewModelScope.launch {
            clearCompletedItems()
        }
    }
}
