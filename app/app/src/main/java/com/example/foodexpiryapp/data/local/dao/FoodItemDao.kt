package com.example.foodexpiryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.foodexpiryapp.data.local.database.FoodItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for FoodItemEntity.
 * All queries return Flow for reactive UI updates.
 */
@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_items ORDER BY expiryDate ASC")
    fun getAllFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: Long): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE expiryDate <= :date ORDER BY expiryDate ASC")
    fun getExpiringBefore(date: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY expiryDate ASC")
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE location = :location ORDER BY expiryDate ASC")
    fun getFoodItemsByLocation(location: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY expiryDate ASC")
    fun searchFoodItems(query: String): Flow<List<FoodItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity): Long

    @Update
    suspend fun updateFoodItem(foodItem: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodItemById(id: Long)

    @Query("SELECT COUNT(*) FROM food_items")
    fun getFoodItemCount(): Flow<Int>

    /** Used by the notification worker to check items expiring soon (non-Flow) */
    @Query("SELECT * FROM food_items WHERE expiryDate <= :date ORDER BY expiryDate ASC")
    suspend fun getExpiringBeforeSync(date: String): List<FoodItemEntity>
}
