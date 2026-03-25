package com.example.foodexpiryapp.data.local.dao

import androidx.room.*
import com.example.foodexpiryapp.data.local.database.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, id DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun clearCompletedItems()
}
