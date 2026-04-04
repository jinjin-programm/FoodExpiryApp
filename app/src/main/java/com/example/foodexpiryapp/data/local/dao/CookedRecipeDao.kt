package com.example.foodexpiryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.foodexpiryapp.data.local.database.CookedRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CookedRecipeDao {
    @Insert
    suspend fun insert(cookedRecipe: CookedRecipeEntity)

    @Query("SELECT * FROM cooked_recipes ORDER BY cookedAt DESC")
    fun getAllCookedRecipes(): Flow<List<CookedRecipeEntity>>

    @Query("SELECT * FROM cooked_recipes ORDER BY cookedAt DESC LIMIT :limit")
    fun getRecentCookedRecipes(limit: Int): Flow<List<CookedRecipeEntity>>

    @Query("SELECT COUNT(*) FROM cooked_recipes")
    fun getCookedRecipesCount(): Flow<Int>

    @Query("SELECT SUM(moneySaved) FROM cooked_recipes")
    fun getTotalMoneySaved(): Flow<Double?>

    @Query("SELECT AVG(wasteRescuedPercent) FROM cooked_recipes")
    fun getAverageWasteRescued(): Flow<Double?>

    @Query("DELETE FROM cooked_recipes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM cooked_recipes")
    suspend fun deleteAll()
}