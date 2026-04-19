package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.util.FoodImageStorage
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/** Returns all food items sorted by expiry date. */
class GetAllFoodItemsUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(): Flow<List<FoodItem>> = repository.getAllFoodItems()
}

/** Returns food items expiring within [days] days from today. */
class GetExpiringFoodItemsUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(days: Int = 3): Flow<List<FoodItem>> {
        val cutoff = LocalDate.now().plusDays(days.toLong())
        return repository.getExpiringBefore(cutoff)
    }
}

/** Adds a new food item. Returns the generated id. */
class AddFoodItemUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    suspend operator fun invoke(foodItem: FoodItem): Long {
        require(foodItem.name.isNotBlank()) { "Food item name cannot be blank" }
        require(foodItem.quantity > 0) { "Quantity must be positive" }
        return repository.insertFoodItem(foodItem)
    }
}

/** Updates an existing food item. */
class UpdateFoodItemUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    suspend operator fun invoke(foodItem: FoodItem) {
        require(foodItem.name.isNotBlank()) { "Food item name cannot be blank" }
        require(foodItem.quantity > 0) { "Quantity must be positive" }
        repository.updateFoodItem(foodItem)
    }
}

/** Deletes a food item and its associated image file. */
class DeleteFoodItemUseCase @Inject constructor(
    private val repository: FoodRepository,
    private val foodImageStorage: FoodImageStorage
) {
    suspend operator fun invoke(foodItem: FoodItem) {
        foodImageStorage.deleteImage(foodItem.imagePath)
        repository.deleteFoodItem(foodItem)
    }

    suspend fun byId(id: Long) {
        val item = repository.getFoodItemById(id)
        if (item != null) {
            foodImageStorage.deleteImage(item.imagePath)
        }
        repository.deleteFoodItemById(id)
    }
}

/** Searches food items by name. */
class SearchFoodItemsUseCase @Inject constructor(
    private val repository: FoodRepository
) {
    operator fun invoke(query: String): Flow<List<FoodItem>> {
        return repository.searchFoodItems(query)
    }
}
