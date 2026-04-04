package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.CookedRecipeDao
import com.example.foodexpiryapp.data.local.database.CookedRecipeEntity
import com.example.foodexpiryapp.domain.repository.CookedRecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookedRecipeRepositoryImpl @Inject constructor(
    private val cookedRecipeDao: CookedRecipeDao
) : CookedRecipeRepository {

    override suspend fun saveCookedRecipe(cookedRecipe: CookedRecipeEntity) {
        cookedRecipeDao.insert(cookedRecipe)
    }

    override fun getAllCookedRecipes(): Flow<List<CookedRecipeEntity>> {
        return cookedRecipeDao.getAllCookedRecipes()
    }

    override fun getRecentCookedRecipes(limit: Int): Flow<List<CookedRecipeEntity>> {
        return cookedRecipeDao.getRecentCookedRecipes(limit)
    }

    override fun getCookedRecipesCount(): Flow<Int> {
        return cookedRecipeDao.getCookedRecipesCount()
    }

    override fun getTotalMoneySaved(): Flow<Double> {
        return cookedRecipeDao.getTotalMoneySaved().map { it ?: 0.0 }
    }

    override fun getAverageWasteRescued(): Flow<Int> {
        return cookedRecipeDao.getAverageWasteRescued().map { (it ?: 0.0).toInt() }
    }

    override suspend fun deleteCookedRecipe(id: Long) {
        cookedRecipeDao.delete(id)
    }

    override suspend fun deleteAll() {
        cookedRecipeDao.deleteAll()
    }
}