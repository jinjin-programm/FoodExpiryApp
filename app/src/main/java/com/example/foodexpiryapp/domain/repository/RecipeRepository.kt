package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeMatch
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    suspend fun getRecipeById(id: Long): Recipe?
    fun searchRecipes(query: String): Flow<List<Recipe>>
    fun getRecipesByTag(tag: String): Flow<List<Recipe>>
    fun getRecipesMatchingInventory(inventoryItemNames: List<String>): Flow<List<Recipe>>
    fun getRandomRecipes(count: Int = 10): Flow<List<Recipe>>
}