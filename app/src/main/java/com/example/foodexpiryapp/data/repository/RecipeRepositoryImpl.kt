package com.example.foodexpiryapp.data.repository

import android.content.Context
import com.example.foodexpiryapp.data.remote.TheMealDbApi
import com.example.foodexpiryapp.data.remote.dto.MealDto
import com.example.foodexpiryapp.data.remote.dto.MealResponseDto
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.domain.model.RecipeTag
import com.example.foodexpiryapp.domain.repository.LocalRecipeRepository
import com.example.foodexpiryapp.domain.repository.RecipeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val theMealDbApi: TheMealDbApi,
    private val localRecipeRepository: LocalRecipeRepository
) : RecipeRepository {

    private val cachedDetails = ConcurrentHashMap<Long, Recipe>()
    @Volatile
    private var inventoryCache: Pair<List<String>, List<Recipe>>? = null
    @Volatile
    private var inventoryCacheTime = 0L
    private val cacheTtlMs = 5 * 60 * 1000L

    override fun getAllRecipes(): Flow<List<Recipe>> = flow {
        val apiRecipes = getRandomMeals(10)
        emit(mergeWithLocalRecipes(apiRecipes))
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

    override fun getRecipesByCategory(category: String): Flow<List<Recipe>> = flow {
        try {
            val response = theMealDbApi.filterByCategory(category)
            emit(response.meals?.map { it.toDomain() } ?: emptyList())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun getRecipesByArea(area: String): Flow<List<Recipe>> = flow {
        try {
            val response = theMealDbApi.filterByArea(area)
            val apiRecipes = response.meals?.map { it.toDomain() } ?: emptyList()
            val localRecipes = try {
                localRecipeRepository.getAllLocalRecipes().first()
                    .filter { it.cuisine.equals(area, ignoreCase = true) }
            } catch (_: Exception) { emptyList() }
            emit(localRecipes + apiRecipes)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun getRecipesMatchingInventory(inventoryItemNames: List<String>): Flow<List<Recipe>> = flow {
        val sortedNames = inventoryItemNames.map { it.trim().lowercase() }.sorted()
        val now = System.currentTimeMillis()
        val cached = inventoryCache
        if (cached != null && cached.first == sortedNames && (now - inventoryCacheTime) < cacheTtlMs) {
            emit(cached.second)
            return@flow
        }

        val allMatchingRecipes = mutableListOf<Recipe>()
        val seenIds = mutableSetOf<Long>()

        val topIngredients = inventoryItemNames
            .map { it.trim().lowercase().replaceFirstChar { char -> char.titlecase() } }
            .take(3)

        if (topIngredients.isEmpty()) {
            val result = getRandomMeals(10)
            emit(result)
            return@flow
        }

        for (ingredient in topIngredients) {
            try {
                var response = theMealDbApi.filterByIngredient(ingredient)

                if (response.meals.isNullOrEmpty()) {
                    response = theMealDbApi.searchMeals(ingredient)
                }

                for (mealDto in response.meals ?: emptyList()) {
                    if (seenIds.size >= 10) break
                    val recipeId = mealDto.idMeal.toLongOrNull() ?: 0L
                    if (!seenIds.add(recipeId)) continue
                    val cachedRecipe = cachedDetails[recipeId]
                    if (cachedRecipe != null) {
                        allMatchingRecipes.add(cachedRecipe)
                    } else {
                        try {
                            val detailResponse = theMealDbApi.getMealDetails(mealDto.idMeal)
                            val detailedRecipe = detailResponse.meals?.firstOrNull()?.toDomain()
                            if (detailedRecipe != null) {
                                cachedDetails[recipeId] = detailedRecipe
                                allMatchingRecipes.add(detailedRecipe)
                            }
                        } catch (_: Exception) {
                            val fallbackRecipe = Recipe(
                                id = recipeId,
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
                            allMatchingRecipes.add(fallbackRecipe)
                        }
                    }
                }
            } catch (e: Exception) {}
            if (allMatchingRecipes.size >= 10) break
        }

        if (allMatchingRecipes.size < 5) {
            val randomOnes = getRandomMeals(5)
            randomOnes.forEach { if (seenIds.add(it.id)) allMatchingRecipes.add(it) }
        }

        val result = allMatchingRecipes.shuffled()
        inventoryCache = sortedNames to result
        inventoryCacheTime = System.currentTimeMillis()
        emit(mergeWithLocalRecipes(result))
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

    private suspend fun mergeWithLocalRecipes(apiRecipes: List<Recipe>): List<Recipe> {
        return try {
            val localRecipes = localRecipeRepository.getAllLocalRecipes().first()
            localRecipes + apiRecipes
        } catch (_: Exception) {
            apiRecipes
        }
    }
}