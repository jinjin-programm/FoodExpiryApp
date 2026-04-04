package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.Recipe
import kotlinx.coroutines.flow.Flow

interface LocalRecipeRepository {
    fun getAllLocalRecipes(): Flow<List<Recipe>>
    suspend fun getLocalRecipeById(id: Long): Recipe?
    suspend fun saveLocalRecipe(recipe: Recipe): Long
    suspend fun updateLocalRecipe(recipe: Recipe)
    suspend fun deleteLocalRecipe(id: Long)
}
