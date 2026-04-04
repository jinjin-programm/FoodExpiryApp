package com.example.foodexpiryapp.data.local.dao

import androidx.room.*
import com.example.foodexpiryapp.data.local.database.LocalRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRecipeDao {
    @Query("SELECT * FROM local_recipes ORDER BY createdAt DESC")
    fun getAllLocalRecipes(): Flow<List<LocalRecipeEntity>>

    @Query("SELECT * FROM local_recipes WHERE id = :id")
    suspend fun getLocalRecipeById(id: Long): LocalRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalRecipe(recipe: LocalRecipeEntity): Long

    @Update
    suspend fun updateLocalRecipe(recipe: LocalRecipeEntity)

    @Delete
    suspend fun deleteLocalRecipe(recipe: LocalRecipeEntity)

    @Query("DELETE FROM local_recipes WHERE id = :id")
    suspend fun deleteLocalRecipeById(id: Long)
}
