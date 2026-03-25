package com.example.foodexpiryapp.data.repository

import android.content.Context
import com.example.foodexpiryapp.data.remote.TheMealDbApi
import com.example.foodexpiryapp.data.remote.dto.MealDto
import com.example.foodexpiryapp.data.remote.dto.MealResponseDto
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.domain.model.RecipeTag
import com.example.foodexpiryapp.domain.repository.RecipeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val theMealDbApi: TheMealDbApi
) : RecipeRepository {

    private val cachedDetails = ConcurrentHashMap<Long, Recipe>()

    override fun getAllRecipes(): Flow<List<Recipe>> = flow {
        emit(getRandomMeals(10))
    }.flowOn(Dispatchers.IO)

    override suspend fun getRecipeById(id: Long): Recipe? {
        cachedDetails[id]?.let { return it }
        return try {
            val response = theMealDbApi.getMealDetails(id.toString())
            val domain = response.meals?.firstOrNull()?.toDomain()
            if (domain != null) {
                cachedDetails[id] = domain
            }
            domain
        } catch (e: Exception) {
            null
        }
    }

    override fun searchRecipes(query: String): Flow<List<Recipe>> = flow {
        try {
            val response = theMealDbApi.searchMeals(query)
            emit(response.meals?.map { it.toDomain() } ?: emptyList())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun getRecipesByTag(tag: String): Flow<List<Recipe>> = flow {
        try {
            val response = theMealDbApi.searchMeals(tag) 
            emit(response.meals?.map { it.toDomain() } ?: emptyList())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun getRecipesMatchingInventory(inventoryItemNames: List<String>): Flow<List<Recipe>> = flow {
        val allMatchingRecipes = mutableListOf<Recipe>()
        val seenIds = mutableSetOf<Long>()

        // Take top ingredients and normalize them (Capitalize first letter)
        val topIngredients = inventoryItemNames
            .map { it.trim().lowercase().replaceFirstChar { char -> char.titlecase() } }
            .take(5)

        if (topIngredients.isEmpty()) {
            emit(getRandomMeals(10))
            return@flow
        }

        for (ingredient in topIngredients) {
            try {
                var response = theMealDbApi.filterByIngredient(ingredient)
                
                // Fallback: If no results for ingredient filter, try a general search
                if (response.meals.isNullOrEmpty()) {
                    response = theMealDbApi.searchMeals(ingredient)
                }

                response.meals?.forEach { mealDto ->
                    val recipe = Recipe(
                        id = mealDto.idMeal.toLongOrNull() ?: 0L,
                        name = mealDto.strMeal,
                        description = "Uses your $ingredient",
                        imageUrl = mealDto.strMealThumb,
                        ingredients = emptyList(),
                        steps = emptyList(),
                        cuisine = mealDto.strArea ?: "",
                        tags = emptySet(),
                        prepTimeMinutes = 15,
                        cookTimeMinutes = 30
                    )
                    if (seenIds.add(recipe.id)) {
                        allMatchingRecipes.add(recipe)
                    }
                }
            } catch (e: Exception) {}
            if (allMatchingRecipes.size >= 20) break
        }

        if (allMatchingRecipes.size < 5) {
            val randomOnes = getRandomMeals(10 - allMatchingRecipes.size)
            randomOnes.forEach { if (seenIds.add(it.id)) allMatchingRecipes.add(it) }
        }

        emit(allMatchingRecipes.shuffled())
    }.flowOn(Dispatchers.IO)

    override fun getRandomRecipes(count: Int): Flow<List<Recipe>> = flow {
        emit(getRandomMeals(count))
    }.flowOn(Dispatchers.IO)

    private suspend fun getRandomMeals(count: Int): List<Recipe> {
        val randomMeals = mutableListOf<Recipe>()
        val seenIds = mutableSetOf<Long>()
        
        repeat(count) {
            try {
                val response = theMealDbApi.getRandomMeal()
                response.meals?.firstOrNull()?.let { mealDto ->
                    val domain = mealDto.toDomain()
                    if (seenIds.add(domain.id)) {
                        randomMeals.add(domain)
                    }
                }
            } catch (e: Exception) {}
        }
        return randomMeals
    }
}
