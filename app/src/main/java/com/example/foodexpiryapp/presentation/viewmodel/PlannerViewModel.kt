package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.MealItemType
import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeMatch
import com.example.foodexpiryapp.domain.usecase.DeleteMealPlanUseCase
import com.example.foodexpiryapp.domain.usecase.GetAllFoodItemsUseCase
import com.example.foodexpiryapp.domain.usecase.GetAllRecipesUseCase
import com.example.foodexpiryapp.domain.usecase.GetMealPlansForDateUseCase
import com.example.foodexpiryapp.domain.usecase.SaveMealPlanUseCase
import com.example.foodexpiryapp.domain.usecase.ScoreRecipesForInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PlannerUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val mealPlans: Map<MealSlot, MealPlan?> = emptyMap(),
    val allRecipes: List<Recipe> = emptyList(),
    val inventoryItems: List<FoodItem> = emptyList(),
    val suggestedRecipes: List<RecipeMatch> = emptyList(),
    val searchQuery: String = "",
    val searchResults: SearchResult = SearchResult.Empty,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed class SearchResult {
    object Empty : SearchResult()
    data class Recipes(val recipes: List<Recipe>) : SearchResult()
    data class Products(val products: List<FoodItem>) : SearchResult()
}

sealed class PlannerEvent {
    data class ShowMessage(val message: String) : PlannerEvent()
    data class MealAdded(val slot: MealSlot, val name: String) : PlannerEvent()
    data class MealDeleted(val slot: MealSlot) : PlannerEvent()
}

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val getMealPlansForDate: GetMealPlansForDateUseCase,
    private val saveMealPlan: SaveMealPlanUseCase,
    private val deleteMealPlan: DeleteMealPlanUseCase,
    private val getAllRecipes: GetAllRecipesUseCase,
    private val getAllFoodItems: GetAllFoodItemsUseCase,
    private val scoreRecipesForInventory: ScoreRecipesForInventoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PlannerEvent>()
    val events: SharedFlow<PlannerEvent> = _events.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                _selectedDate.flatMapLatest { date -> getMealPlansForDate(date) },
                getAllRecipes(),
                getAllFoodItems(),
                _searchQuery,
                _selectedDate
            ) { mealPlans, recipes, inventory, query, selectedDate ->
                val plansMap = MealSlot.entries.associateWith { slot ->
                    mealPlans.find { it.slot == slot }
                }

                val searchResult = when {
                    query.isBlank() -> SearchResult.Empty
                    else -> {
                        val lowerQuery = query.lowercase()
                        val matchingRecipes = recipes.filter { recipe ->
                            recipe.name.lowercase().contains(lowerQuery) ||
                            recipe.ingredients.any { it.name.lowercase().contains(lowerQuery) }
                        }
                        val matchingProducts = inventory.filter {
                            it.name.lowercase().contains(lowerQuery)
                        }
                        if (matchingRecipes.isNotEmpty()) {
                            SearchResult.Recipes(matchingRecipes)
                        } else if (matchingProducts.isNotEmpty()) {
                            SearchResult.Products(matchingProducts)
                        } else {
                            SearchResult.Empty
                        }
                    }
                }

                val allMatchedRecipes = scoreRecipesForInventory(recipes, inventory)
                val suggestedRecipes = allMatchedRecipes.filter { match ->
                    match.matchedInventoryItems.any { it.daysUntilExpiry <= 7 }
                }

                PlannerUiState(
                    selectedDate = selectedDate,
                    mealPlans = plansMap,
                    allRecipes = recipes,
                    inventoryItems = inventory,
                    suggestedRecipes = suggestedRecipes,
                    searchQuery = query,
                    searchResults = searchResult,
                    isLoading = false,
                    errorMessage = null
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAddRecipe(slot: MealSlot, recipe: Recipe) {
        viewModelScope.launch {
            val mealPlan = MealPlan(
                date = _uiState.value.selectedDate,
                slot = slot,
                itemType = MealItemType.RECIPE,
                recipeId = recipe.id,
                recipeName = recipe.name,
                productName = null,
                inventoryItemId = null
            )
            saveMealPlan(mealPlan)
            _events.emit(PlannerEvent.MealAdded(slot, recipe.name))
        }
    }

    fun onAddProduct(slot: MealSlot, product: FoodItem) {
        viewModelScope.launch {
            val mealPlan = MealPlan(
                date = _uiState.value.selectedDate,
                slot = slot,
                itemType = MealItemType.PRODUCT,
                recipeId = null,
                recipeName = null,
                productName = product.name,
                inventoryItemId = product.id
            )
            saveMealPlan(mealPlan)
            _events.emit(PlannerEvent.MealAdded(slot, product.name))
        }
    }

    fun onAddCustomProduct(slot: MealSlot, productName: String) {
        viewModelScope.launch {
            val mealPlan = MealPlan(
                date = _uiState.value.selectedDate,
                slot = slot,
                itemType = MealItemType.PRODUCT,
                recipeId = null,
                recipeName = null,
                productName = productName,
                inventoryItemId = null
            )
            saveMealPlan(mealPlan)
            _events.emit(PlannerEvent.MealAdded(slot, productName))
        }
    }

    fun onDeleteMeal(slot: MealSlot) {
        viewModelScope.launch {
            _uiState.value.mealPlans[slot]?.let { plan ->
                deleteMealPlan(plan)
                _events.emit(PlannerEvent.MealDeleted(slot))
            }
        }
    }

    fun getFilteredRecipes(query: String): List<Recipe> {
        if (query.isBlank()) return _uiState.value.allRecipes
        val lowerQuery = query.lowercase()
        return _uiState.value.allRecipes.filter { recipe ->
            recipe.name.lowercase().contains(lowerQuery) ||
            recipe.ingredients.any { it.name.lowercase().contains(lowerQuery) }
        }
    }

    fun getFilteredProducts(query: String): List<FoodItem> {
        if (query.isBlank()) return _uiState.value.inventoryItems
        val lowerQuery = query.lowercase()
        return _uiState.value.inventoryItems.filter {
            it.name.lowercase().contains(lowerQuery)
        }
    }

    fun getSuggestedRecipes(): List<RecipeMatch> {
        return _uiState.value.suggestedRecipes
    }

    fun getFilteredRecipesWithMatchInfo(query: String): List<RecipeMatch> {
        val suggested = _uiState.value.suggestedRecipes
        if (query.isBlank()) return suggested
        val lowerQuery = query.lowercase()
        return suggested.filter { match ->
            match.recipe.name.lowercase().contains(lowerQuery) ||
            match.matchedIngredients.any { it.name.lowercase().contains(lowerQuery) }
        }
    }
}
