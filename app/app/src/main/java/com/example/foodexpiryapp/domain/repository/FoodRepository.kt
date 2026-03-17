package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for food items.
 * Defined in the domain layer -- implementation lives in the data layer.
 */
interface FoodRepository {

    fun getAllFoodItems(): Flow<List<FoodItem>>

    suspend fun getFoodItemById(id: Long): FoodItem?

    fun getExpiringBefore(date: LocalDate): Flow<List<FoodItem>>

    fun getFoodItemsByCategory(category: FoodCategory): Flow<List<FoodItem>>

    fun getFoodItemsByLocation(location: StorageLocation): Flow<List<FoodItem>>

    fun searchFoodItems(query: String): Flow<List<FoodItem>>

    suspend fun insertFoodItem(foodItem: FoodItem): Long

    suspend fun updateFoodItem(foodItem: FoodItem)

    suspend fun deleteFoodItem(foodItem: FoodItem)

    suspend fun deleteFoodItemById(id: Long)

    fun getFoodItemCount(): Flow<Int>

    /** Non-Flow version used by background workers (notifications). */
    suspend fun getExpiringBeforeSync(date: LocalDate): List<FoodItem>
}
