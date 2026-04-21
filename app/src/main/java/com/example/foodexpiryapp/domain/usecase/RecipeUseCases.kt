package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.domain.model.RecipeMatch
import com.example.foodexpiryapp.domain.model.UserAllergens
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.domain.repository.LocalRecipeRepository
import com.example.foodexpiryapp.domain.repository.RecipeRepository
import com.example.foodexpiryapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetAllRecipesUseCase @Inject constructor(
    private val repository: RecipeRepository
) {
    operator fun invoke(): Flow<List<Recipe>> = repository.getAllRecipes()
}

class GetRecipeByIdUseCase @Inject constructor(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(id: Long): Recipe? = repository.getRecipeById(id)
}

class SearchRecipesUseCase @Inject constructor(
    private val repository: RecipeRepository
) {
    operator fun invoke(query: String): Flow<List<Recipe>> = repository.searchRecipes(query)
}

class GetRecipesMatchingInventoryUseCase @Inject constructor(
    private val repository: RecipeRepository
) {
    operator fun invoke(inventoryItems: List<FoodItem>): Flow<List<Recipe>> {
        val names = inventoryItems.map { it.name.lowercase() }
        return repository.getRecipesMatchingInventory(names)
    }
}

class ScoreRecipesForInventoryUseCase @Inject constructor(
    private val repository: RecipeRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(recipes: List<Recipe>, inventoryItems: List<FoodItem>): List<RecipeMatch> {
        val profile = userRepository.getUserProfile().first()
        val allergenNames = profile.allergens.presetAllergens.map { it.displayName.lowercase() } +
                         profile.allergens.customAllergens.map { it.lowercase() }
        
        val filteredRecipes = if (allergenNames.isNotEmpty()) {
            recipes.filter { recipe ->
                recipe.ingredients.none { ing ->
                    allergenNames.any { allergen ->
                        ing.name.lowercase() == allergen
                    }
                }
            }
        } else {
            recipes
        }
        
        return filteredRecipes.map { recipe ->
            val matchedInvItems = recipe.matchedInventoryItems(inventoryItems)
            val matchedIngs = recipe.ingredients.filter { ing ->
                matchedInvItems.any { item -> 
                    ing.name.lowercase().contains(item.name.lowercase()) || 
                    item.name.lowercase().contains(ing.name.lowercase())
                }
            }
            
            val avgItemCost = 3.0
            val estimatedSaved = matchedInvItems.sumOf { avgItemCost * 0.7 }
            val wasteRescue = (matchedInvItems.size * 25).coerceAtMost(100)
            
            RecipeMatch(
                recipe = recipe,
                matchCount = matchedInvItems.size,
                matchedIngredients = matchedIngs,
                matchedInventoryItems = matchedInvItems,
                estimatedMoneySaved = estimatedSaved,
                wasteRescuePercent = wasteRescue,
                urgencyLevel = recipe.urgencyScore(),
                dietaryFlags = recipe.tags.filter { 
                    listOf("VEGETARIAN","VEGAN","GLUTEN_FREE","DAIRY_FREE","NUT_FREE","LOW_CARB").contains(it.name)
                }.toList()
            )
        }.sortedByDescending { match ->
            val expiryUrgencyBonus = match.matchedInventoryItems.sumOf { item ->
                when {
                    item.isExpired -> 10L
                    item.daysUntilExpiry <= 1 -> 8L
                    item.daysUntilExpiry <= 3 -> 5L
                    item.daysUntilExpiry <= 7 -> 3L
                    else -> 1L
                }
            }
            match.matchCount * 10 + match.urgencyLevel * 5 + match.wasteRescuePercent + expiryUrgencyBonus
        }
    }
}

class ConsumeIngredientsUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val foodImageStorage: com.example.foodexpiryapp.util.FoodImageStorage
) {
    suspend operator fun invoke(matchedItems: List<FoodItem>) {
        for (item in matchedItems) {
            if (item.quantity > 1) {
                foodRepository.updateFoodItem(item.copy(quantity = item.quantity - 1))
            } else {
                foodImageStorage.deleteImage(item.imagePath)
                foodRepository.deleteFoodItem(item)
            }
        }
    }
}

class GetRecipesByCategoryUseCase @Inject constructor(
    private val repository: RecipeRepository
) {
    operator fun invoke(category: String): Flow<List<Recipe>> = repository.getRecipesByCategory(category)
}

class GetRecipesByAreaUseCase @Inject constructor(
    private val repository: RecipeRepository
) {
    operator fun invoke(area: String): Flow<List<Recipe>> = repository.getRecipesByArea(area)
}

class SaveLocalRecipeUseCase @Inject constructor(
    private val repository: LocalRecipeRepository
) {
    suspend operator fun invoke(recipe: Recipe): Long = repository.saveLocalRecipe(recipe)
}

class GetAllLocalRecipesUseCase @Inject constructor(
    private val repository: LocalRecipeRepository
) {
    operator fun invoke(): Flow<List<Recipe>> = repository.getAllLocalRecipes()
}

class FilterRecipesByAllergensUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(recipes: List<Recipe>): List<Recipe> {
        val profile = userRepository.getUserProfile().first()
        val allergenNames = profile.allergens.presetAllergens.map { it.displayName.lowercase() } +
                         profile.allergens.customAllergens.map { it.lowercase() }
        
        if (allergenNames.isEmpty()) return recipes
        
        return recipes.filter { recipe ->
            recipe.ingredients.none { ing ->
                allergenNames.any { allergen ->
                    ing.name.lowercase() == allergen
                }
            }
        }
    }
}
