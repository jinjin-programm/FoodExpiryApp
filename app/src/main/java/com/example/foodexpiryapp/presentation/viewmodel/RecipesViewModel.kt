package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeMatch
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.domain.usecase.GetAllFoodItemsUseCase
import com.example.foodexpiryapp.domain.usecase.GetAllRecipesUseCase
import com.example.foodexpiryapp.domain.usecase.ScoreRecipesForInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
    val allRecipes: List<Recipe> = emptyList(),
    val recipeMatches: List<RecipeMatch> = emptyList(),
    val expiringItems: List<FoodItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: RecipeFilter = RecipeFilter.ALL,
    val totalMoneySaved: Double = 0.0,
    val totalWasteRescued: Int = 0,
    val recipesUsedCount: Int = 0,
    val errorMessage: String? = null
)

enum class RecipeFilter(val displayName: String) {
    ALL("All Recipes"),
    BEST_MATCH("Best Match"),
    USE_SOON("Use Soon"),
    WASTE_BUSTER("Waste Buster"),
    QUICK("Quick (< 30 min)"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    DAIRY_FREE("Dairy Free"),
    GLUTEN_FREE("Gluten Free")
}

sealed class RecipesEvent {
    data class ShowMessage(val message: String) : RecipesEvent()
    data class RecipeViewed(val recipe: Recipe, val savedAmount: Double) : RecipesEvent()
}

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val getAllRecipes: GetAllRecipesUseCase,
    private val getAllFoodItems: GetAllFoodItemsUseCase,
    private val scoreRecipesForInventory: ScoreRecipesForInventoryUseCase,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecipesEvent>()
    val events: SharedFlow<RecipesEvent> = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(RecipeFilter.ALL)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                getAllRecipes(),
                getAllFoodItems(),
                _searchQuery,
                _selectedFilter
            ) { recipes, inventoryItems, query, filter ->
                val expiringItems = inventoryItems.filter { it.daysUntilExpiry <= 3 && !it.isExpired }
                    .sortedBy { it.daysUntilExpiry }

                val allMatches = scoreRecipesForInventory(recipes, inventoryItems)

                val filteredMatches = applyFilter(allMatches, filter, query)

                val totalSaved = allMatches.sumOf { it.estimatedMoneySaved }
                val totalWaste = allMatches.sumOf { it.wasteRescuePercent } / (allMatches.size.coerceAtLeast(1))

                Triple(filteredMatches, expiringItems, Pair(totalSaved, totalWaste))
            }.collect { (matches, expiring, stats) ->
                _uiState.update { state ->
                    state.copy(
                        allRecipes = state.allRecipes,
                        recipeMatches = matches,
                        expiringItems = expiring,
                        isLoading = false,
                        totalMoneySaved = stats.first,
                        totalWasteRescued = stats.second,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun applyFilter(
        matches: List<RecipeMatch>,
        filter: RecipeFilter,
        query: String
    ): List<RecipeMatch> {
        var result = when (filter) {
            RecipeFilter.ALL -> matches
            RecipeFilter.BEST_MATCH -> matches.sortedByDescending { it.matchCount * 10 + it.wasteRescuePercent }
            RecipeFilter.USE_SOON -> matches.filter { it.recipe.tags.any { t -> t == com.example.foodexpiryapp.domain.model.RecipeTag.USE_SOON || t == com.example.foodexpiryapp.domain.model.RecipeTag.URGENT } }
            RecipeFilter.WASTE_BUSTER -> matches.filter { it.recipe.tags.contains(com.example.foodexpiryapp.domain.model.RecipeTag.WASTE_BUSTER) }
            RecipeFilter.QUICK -> matches.filter { it.recipe.totalTimeMinutes < 30 }
            RecipeFilter.VEGETARIAN -> matches.filter { it.recipe.tags.contains(com.example.foodexpiryapp.domain.model.RecipeTag.VEGETARIAN) }
            RecipeFilter.VEGAN -> matches.filter { it.recipe.tags.contains(com.example.foodexpiryapp.domain.model.RecipeTag.VEGAN) }
            RecipeFilter.DAIRY_FREE -> matches.filter { it.recipe.tags.contains(com.example.foodexpiryapp.domain.model.RecipeTag.DAIRY_FREE) }
            RecipeFilter.GLUTEN_FREE -> matches.filter { it.recipe.tags.contains(com.example.foodexpiryapp.domain.model.RecipeTag.GLUTEN_FREE) }
        }

        if (query.isNotBlank()) {
            val lower = query.lowercase()
            result = result.filter { match ->
                match.recipe.name.lowercase().contains(lower) ||
                match.recipe.description.lowercase().contains(lower) ||
                match.matchedIngredients.any { it.name.lowercase().contains(lower) }
            }
        }

        return result
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChanged(filter: RecipeFilter) {
        _selectedFilter.value = filter
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onRecipeViewed(recipe: Recipe, matchedItems: List<FoodItem>) {
        viewModelScope.launch {
            val savedAmount = recipe.estimatedSaving * (matchedItems.size.toDouble() / recipe.ingredients.size.coerceAtLeast(1))

            analyticsRepository.trackEvent(
                AnalyticsEvent(
                    eventName = "recipe_viewed",
                    eventType = EventType.SCREEN_VIEW,
                    itemName = recipe.name,
                    additionalData = mapOf(
                        "recipe_id" to recipe.id.toString(),
                        "matched_items" to matchedItems.joinToString(",") { it.name },
                        "money_saved_estimate" to savedAmount.toString(),
                        "waste_rescue_percent" to recipe.wasteRescueScore.toString()
                    )
                )
            )

            _events.emit(RecipesEvent.RecipeViewed(recipe, savedAmount))
        }
    }

    fun onRecipeCooked(recipe: Recipe, matchedItems: List<FoodItem>) {
        viewModelScope.launch {
            val savedAmount = recipe.estimatedSaving * (matchedItems.size.toDouble() / recipe.ingredients.size.coerceAtLeast(1))

            analyticsRepository.trackEvent(
                AnalyticsEvent(
                    eventName = "recipe_cooked",
                    eventType = EventType.ITEM_EATEN,
                    itemName = recipe.name,
                    additionalData = mapOf(
                        "recipe_id" to recipe.id.toString(),
                        "items_rescued" to matchedItems.joinToString(",") { it.name },
                        "estimated_money_saved" to savedAmount.toString(),
                        "waste_rescue_percent" to recipe.wasteRescueScore.toString()
                    )
                )
            )

            _uiState.update { it.copy(recipesUsedCount = it.recipesUsedCount + 1) }
            _events.emit(RecipesEvent.ShowMessage("Recipe cooked! Saved ~$${String.format("%.2f", savedAmount)}!"))
        }
    }
}