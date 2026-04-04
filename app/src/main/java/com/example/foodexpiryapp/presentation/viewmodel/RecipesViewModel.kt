package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.*
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.data.local.database.CookedRecipeEntity
import com.example.foodexpiryapp.domain.repository.CookedRecipeRepository
import com.example.foodexpiryapp.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipesUiState(
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
    GLUTEN_FREE("Gluten Free"),
    BREAKFAST("Breakfast"),
    DESSERT("Dessert"),
    CHINESE("Chinese"),
    MEXICAN("Mexican"),
    JAPANESE("Japanese"),
    INDIAN("Indian"),
    ITALIAN("Italian")
}

sealed class RecipesEvent {
    data class ShowMessage(val message: String) : RecipesEvent()
    data class RecipeViewed(val recipe: Recipe, val savedAmount: Double) : RecipesEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val getAllRecipes: GetAllRecipesUseCase,
    private val getAllFoodItems: GetAllFoodItemsUseCase,
    private val scoreRecipesForInventory: ScoreRecipesForInventoryUseCase,
    private val searchRecipes: SearchRecipesUseCase,
    private val getRecipesMatchingInventory: GetRecipesMatchingInventoryUseCase,
    private val getRecipesByCategory: GetRecipesByCategoryUseCase,
    private val getRecipesByArea: GetRecipesByAreaUseCase,
    private val consumeIngredients: ConsumeIngredientsUseCase,
    private val cookedRecipeRepository: CookedRecipeRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(RecipeFilter.ALL)
    private val _loadMoreTrigger = MutableStateFlow(0)
    
    private val _events = MutableSharedFlow<RecipesEvent>()
    val events: SharedFlow<RecipesEvent> = _events.asSharedFlow()

    private val allFetchedRecipes = MutableStateFlow<List<Recipe>>(emptyList())

    private val inventoryItems = getAllFoodItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val cookedStats = combine(
        cookedRecipeRepository.getCookedRecipesCount(),
        cookedRecipeRepository.getTotalMoneySaved(),
        cookedRecipeRepository.getAverageWasteRescued()
    ) { count, money, waste ->
        Triple(count, money, waste)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Triple(0, 0.0, 0))

    private val recipesFlow = combine(inventoryItems, _searchQuery, _selectedFilter, _loadMoreTrigger) { items, query, filter, _ ->
        Triple(items, query, filter)
    }.flatMapLatest { (items, query, filter) ->
        when {
            query.isNotBlank() -> searchRecipes(query)
            filter == RecipeFilter.ALL -> getRecipesMatchingInventory(items)
            filter == RecipeFilter.BREAKFAST -> getRecipesByCategory("Breakfast")
            filter == RecipeFilter.DESSERT -> getRecipesByCategory("Dessert")
            filter == RecipeFilter.CHINESE -> getRecipesByArea("Chinese")
            filter == RecipeFilter.MEXICAN -> getRecipesByArea("Mexican")
            filter == RecipeFilter.JAPANESE -> getRecipesByArea("Japanese")
            filter == RecipeFilter.INDIAN -> getRecipesByArea("Indian")
            filter == RecipeFilter.ITALIAN -> getRecipesByArea("Italian")
            else -> getRecipesMatchingInventory(items)
        }
    }.onEach { newBatch ->
        if (_searchQuery.value.isNotBlank() || _selectedFilter.value != RecipeFilter.ALL) {
             allFetchedRecipes.value = newBatch
        } else {
             allFetchedRecipes.value = (allFetchedRecipes.value + newBatch).distinctBy { it.id }
        }
    }

    init {
        recipesFlow.launchIn(viewModelScope)
    }

    val uiState: StateFlow<RecipesUiState> = combine(
        allFetchedRecipes,
        inventoryItems,
        _searchQuery,
        _selectedFilter,
        cookedStats
    ) { recipes, inventory, query, filter, stats ->
        
        val expiringItems = inventory.filter { it.daysUntilExpiry <= 3 && !it.isExpired }
            .sortedBy { it.daysUntilExpiry }

        val allMatches = scoreRecipesForInventory(recipes, inventory)
        val filteredMatches = applyFilter(allMatches, filter, query, inventory)

        RecipesUiState(
            recipeMatches = filteredMatches,
            expiringItems = expiringItems,
            isLoading = false,
            searchQuery = query,
            selectedFilter = filter,
            totalMoneySaved = stats.second,
            totalWasteRescued = stats.third,
            recipesUsedCount = stats.first,
            errorMessage = null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipesUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        allFetchedRecipes.value = emptyList()
    }

    fun onFilterChanged(filter: RecipeFilter) {
        _selectedFilter.value = filter
        allFetchedRecipes.value = emptyList()
    }

    fun onLoadMoreRequested() {
        if (uiState.value.isLoading) return
        _loadMoreTrigger.value += 1
    }

    private fun applyFilter(
        matches: List<RecipeMatch>,
        filter: RecipeFilter,
        query: String,
        inventory: List<FoodItem>
    ): List<RecipeMatch> {
        var result = when (filter) {
            RecipeFilter.ALL -> matches
            RecipeFilter.BEST_MATCH -> matches.sortedByDescending { it.matchCount * 10 + it.wasteRescuePercent }
            RecipeFilter.USE_SOON -> matches.filter { it.recipe.tags.any { t -> t == RecipeTag.USE_SOON || t == RecipeTag.URGENT } }
            RecipeFilter.WASTE_BUSTER -> matches.filter { it.recipe.tags.contains(RecipeTag.WASTE_BUSTER) }
            RecipeFilter.QUICK -> matches.filter { it.recipe.totalTimeMinutes < 30 }
            RecipeFilter.VEGETARIAN -> matches.filter { it.recipe.tags.contains(RecipeTag.VEGETARIAN) }
            RecipeFilter.VEGAN -> matches.filter { it.recipe.tags.contains(RecipeTag.VEGAN) }
            RecipeFilter.DAIRY_FREE -> matches.filter { it.recipe.tags.contains(RecipeTag.DAIRY_FREE) }
            RecipeFilter.GLUTEN_FREE -> matches.filter { it.recipe.tags.contains(RecipeTag.GLUTEN_FREE) }
            RecipeFilter.BREAKFAST,
            RecipeFilter.DESSERT,
            RecipeFilter.CHINESE,
            RecipeFilter.MEXICAN,
            RecipeFilter.JAPANESE,
            RecipeFilter.INDIAN,
            RecipeFilter.ITALIAN -> matches
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
            val wastePercent = if (matchedItems.isNotEmpty()) {
                ((matchedItems.size.toDouble() / recipe.ingredients.size) * 100).toInt().coerceAtMost(100)
            } else 0

            consumeIngredients(matchedItems)

            val cookedRecipe = CookedRecipeEntity(
                recipeId = recipe.id,
                recipeName = recipe.name,
                moneySaved = savedAmount,
                wasteRescuedPercent = wastePercent,
                matchedIngredients = matchedItems.joinToString(",") { it.name },
                imageUrl = recipe.imageUrl
            )
            cookedRecipeRepository.saveCookedRecipe(cookedRecipe)

            analyticsRepository.trackEvent(
                AnalyticsEvent(
                    eventName = "recipe_cooked",
                    eventType = EventType.ITEM_EATEN,
                    itemName = recipe.name,
                    additionalData = mapOf(
                        "recipe_id" to recipe.id.toString(),
                        "items_rescued" to matchedItems.joinToString(",") { it.name },
                        "estimated_money_saved" to savedAmount.toString(),
                        "waste_rescue_percent" to wastePercent.toString()
                    )
                )
            )

            _events.emit(RecipesEvent.ShowMessage("Recipe cooked! Saved ~$${String.format("%.2f", savedAmount)}!"))
        }
    }
}
