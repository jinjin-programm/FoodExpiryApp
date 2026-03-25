package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val inventoryItems: List<FoodItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class RecipeDetailEvent {
    data class ShowMessage(val message: String) : RecipeDetailEvent()
    object NavigateBack : RecipeDetailEvent()
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val getRecipeById: GetRecipeByIdUseCase,
    private val getAllFoodItems: GetAllFoodItemsUseCase,
    private val addShoppingItem: AddShoppingItemUseCase,
    private val consumeIngredients: ConsumeIngredientsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecipeDetailEvent>()
    val events = _events.asSharedFlow()

    fun loadRecipe(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val recipe = getRecipeById(id)
            if (recipe == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Recipe not found") }
                _events.emit(RecipeDetailEvent.ShowMessage("Recipe details could not be loaded"))
                _events.emit(RecipeDetailEvent.NavigateBack)
                return@launch
            }

            getAllFoodItems().collect { items ->
                _uiState.update { 
                    it.copy(
                        recipe = recipe,
                        inventoryItems = items,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onAddSelectedToShoppingList(selectedIngredients: List<String>) {
        viewModelScope.launch {
            selectedIngredients.forEach { name ->
                addShoppingItem(name)
            }
            _events.emit(RecipeDetailEvent.ShowMessage("${selectedIngredients.size} items added to shopping list"))
        }
    }

    fun onCooked() {
        val recipe = _uiState.value.recipe ?: return
        val inventory = _uiState.value.inventoryItems
        
        viewModelScope.launch {
            // Find which items from inventory match the recipe ingredients
            val matchedItems = recipe.matchedInventoryItems(inventory)
            consumeIngredients(matchedItems)
            _events.emit(RecipeDetailEvent.ShowMessage("Delicious! Inventory updated."))
            _events.emit(RecipeDetailEvent.NavigateBack)
        }
    }
}
