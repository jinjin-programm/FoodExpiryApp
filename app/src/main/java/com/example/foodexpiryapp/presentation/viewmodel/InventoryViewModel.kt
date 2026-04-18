package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.usecase.*
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.domain.repository.CookedRecipeRepository
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.domain.repository.UIStyleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * UI state for the Inventory screen.
 */
data class InventoryUiState(
    val foodItems: List<FoodItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: FoodCategory? = null,
    val selectedLocation: StorageLocation? = null,
    val errorMessage: String? = null,
    val uiStyle: String = UIStyleRepository.STYLE_CUTE
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
    private val searchFoodItems: SearchFoodItemsUseCase,
    private val foodRepository: FoodRepository,
    private val cookedRecipeRepository: CookedRecipeRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val uiStyleRepository: UIStyleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<FoodCategory?>(null)
    private val _selectedLocation = MutableStateFlow<StorageLocation?>(null)

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InventoryEvent>()
    val events: SharedFlow<InventoryEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            uiStyleRepository.uiStyle.collect { style ->
                _uiState.update { it.copy(uiStyle = style) }
            }
        }

        viewModelScope.launch {
            combine(_searchQuery.debounce(300), _selectedCategory, _selectedLocation) { query, category, location ->
                Triple(query, category, location)
            }
            .flatMapLatest { (query, category, location) ->
                if (query.isBlank()) {
                    getAllFoodItems()
                } else {
                    searchFoodItems(query)
                }
                .map { items ->
                    var filtered = items
                    if (category != null) {
                        filtered = filtered.filter { it.category == category }
                    }
                    if (location != null) {
                        filtered = filtered.filter { it.location == location }
                    }
                    filtered
                }
            }
            .collect { items ->
                _uiState.update { state ->
                    state.copy(
                        foodItems = items.sortedWith(compareByDescending<FoodItem> { it.riskLevel }.thenBy { it.daysUntilExpiry }),
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
        
        if (query.isNotBlank() && query.length >= 3) {
            analyticsRepository.trackEvent(
                AnalyticsEvent(
                    eventName = "search_query",
                    eventType = EventType.SEARCH_QUERY,
                    additionalData = mapOf("query" to query)
                )
            )
        }
    }

    fun onCategorySelected(category: FoodCategory?) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onLocationSelected(location: StorageLocation?) {
        _selectedLocation.value = location
        _selectedCategory.value = null
        _uiState.update { it.copy(selectedLocation = location, selectedCategory = null) }
    }

    fun onAddFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                addFoodItem(foodItem)
                
                // Track analytics
                analyticsRepository.trackEvent(
                    AnalyticsEvent(
                        eventName = "item_added",
                        eventType = EventType.ITEM_ADDED,
                        itemName = foodItem.name,
                        itemCategory = foodItem.category.name,
                        itemLocation = foodItem.location.name
                    )
                )
                
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
                
                // Track analytics
                analyticsRepository.trackEvent(
                    AnalyticsEvent(
                        eventName = "item_deleted",
                        eventType = EventType.ITEM_DELETED,
                        itemName = foodItem.name,
                        itemCategory = foodItem.category.name,
                        itemLocation = foodItem.location.name,
                        additionalData = mapOf("reason" to "swipe_delete")
                    )
                )
                
                _events.emit(InventoryEvent.ItemDeleted(foodItem))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }

    /** Mark a food item as eaten. */
    fun onMarkAsEaten(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                val daysBeforeExpiry = foodItem.daysUntilExpiry.toInt()
                
                // Track analytics
                analyticsRepository.trackEvent(
                    AnalyticsEvent(
                        eventName = "item_eaten",
                        eventType = EventType.ITEM_EATEN,
                        itemName = foodItem.name,
                        itemCategory = foodItem.category.name,
                        itemLocation = foodItem.location.name,
                        additionalData = mapOf("days_before_expiry" to daysBeforeExpiry.toString())
                    )
                )
                
                deleteFoodItem(foodItem)
                _events.emit(InventoryEvent.ShowMessage("${foodItem.name} marked as eaten!"))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error marking eaten: ${e.message}"))
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

    fun onInsertTestData() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val items = listOf(
                    FoodItem(name = "Milk", category = FoodCategory.DAIRY, expiryDate = today.plusDays(1), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(3)),
                    FoodItem(name = "Chicken Breast", category = FoodCategory.MEAT, expiryDate = today.plusDays(2), quantity = 2, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(2)),
                    FoodItem(name = "Broccoli", category = FoodCategory.VEGETABLES, expiryDate = today.plusDays(3), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(1)),
                    FoodItem(name = "Apple", category = FoodCategory.FRUITS, expiryDate = today.plusDays(5), quantity = 3, location = StorageLocation.COUNTER, dateAdded = today.minusDays(2)),
                    FoodItem(name = "Rice", category = FoodCategory.GRAINS, expiryDate = today.plusDays(7), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(1)),
                    FoodItem(name = "Yogurt", category = FoodCategory.DAIRY, expiryDate = today.minusDays(1), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(5)),
                    FoodItem(name = "Salmon", category = FoodCategory.MEAT, expiryDate = today.plusDays(1), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(1)),
                    FoodItem(name = "Tomato", category = FoodCategory.VEGETABLES, expiryDate = today.plusDays(4), quantity = 4, location = StorageLocation.COUNTER, dateAdded = today),
                    FoodItem(name = "Cheese", category = FoodCategory.DAIRY, expiryDate = today.plusDays(10), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today),
                    FoodItem(name = "Egg", category = FoodCategory.OTHER, expiryDate = today.plusDays(14), quantity = 6, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(1)),
                    FoodItem(name = "Tofu", category = FoodCategory.OTHER, expiryDate = today.plusDays(2), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today),
                    FoodItem(name = "Lettuce", category = FoodCategory.VEGETABLES, expiryDate = today.minusDays(1), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today.minusDays(4)),
                    FoodItem(name = "Banana", category = FoodCategory.FRUITS, expiryDate = today.plusDays(3), quantity = 2, location = StorageLocation.COUNTER, dateAdded = today.minusDays(2)),
                    FoodItem(name = "Beef", category = FoodCategory.MEAT, expiryDate = today.plusDays(6), quantity = 1, location = StorageLocation.FREEZER, dateAdded = today),
                    FoodItem(name = "Orange Juice", category = FoodCategory.BEVERAGES, expiryDate = today.plusDays(20), quantity = 1, location = StorageLocation.FRIDGE, dateAdded = today)
                )
                items.forEach { addFoodItem(it) }
                _events.emit(InventoryEvent.ShowMessage("Added ${items.size} test food items"))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }

    fun onDeleteAllFoodItems() {
        viewModelScope.launch {
            try {
                foodRepository.deleteAllFoodItems()
                cookedRecipeRepository.deleteAll()
                _events.emit(InventoryEvent.ShowMessage("All data cleared"))
            } catch (e: Exception) {
                _events.emit(InventoryEvent.ShowMessage("Error: ${e.message}"))
            }
        }
    }
}
