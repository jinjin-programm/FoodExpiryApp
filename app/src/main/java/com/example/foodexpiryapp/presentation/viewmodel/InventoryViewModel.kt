package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Inventory screen.
 */
data class InventoryUiState(
    val foodItems: List<FoodItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: FoodCategory? = null,
    val errorMessage: String? = null
)

/**
 * One-shot events sent from ViewModel to the UI.
 */
sealed class InventoryEvent {
    data class ShowMessage(val message: String) : InventoryEvent()
    data class ItemDeleted(val item: FoodItem) : InventoryEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getAllFoodItems: GetAllFoodItemsUseCase,
    private val addFoodItem: AddFoodItemUseCase,
    private val updateFoodItem: UpdateFoodItemUseCase,
    private val deleteFoodItem: DeleteFoodItemUseCase,
    private val searchFoodItems: SearchFoodItemsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events: SharedFlow<InventoryEvent> = _events.asSharedFlow()

    init {
        // React to search query changes: empty query -> all items, otherwise -> search
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        getAllFoodItems()
                    } else {
                        searchFoodItems(query)
                    }
                }
                .collect { items ->
                    _uiState.update { state ->
                        state.copy(
                            foodItems = items,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAddFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                addFoodItem(foodItem)
                _events.emit(InventoryEvent.ShowMessage("${foodItem.name} added"))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }

    fun onUpdateFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                updateFoodItem(foodItem)
                _events.emit(InventoryEvent.ShowMessage("${foodItem.name} updated"))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }

    fun onDeleteFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                deleteFoodItem(foodItem)
                _events.emit(InventoryEvent.ItemDeleted(foodItem))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }

    /** Undo a deletion by re-inserting the item. */
    fun onUndoDelete(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                addFoodItem(foodItem)
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error undoing: ${e.message}"))
            }
        }
    }
}
