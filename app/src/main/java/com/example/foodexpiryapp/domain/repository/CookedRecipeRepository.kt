package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.data.local.database.CookedRecipeEntity
import kotlinx.coroutines.flow.Flow

interface CookedRecipeRepository {
    suspend fun saveCookedRecipe(cookedRecipe: CookedRecipeEntity)
    fun getAllCookedRecipes(): Flow<List<CookedRecipeEntity>>
    fun getRecentCookedRecipes(limit: Int): Flow<List<CookedRecipeEntity>>
    fun getCookedRecipesCount(): Flow<Int>
    fun getTotalMoneySaved(): Flow<Double>
    fun getAverageWasteRescued(): Flow<Int>
    suspend fun deleteCookedRecipe(id: Long)
    suspend fun deleteAll()
}