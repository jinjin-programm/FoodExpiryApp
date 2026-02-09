package com.example.foodexpiryapp.domain.model

import java.time.LocalDate

/**
 * Represents a food item in the user's pantry
 */
data class FoodItem(
    val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val expiryDate: LocalDate,
    val quantity: Int = 1,
    val location: StorageLocation = StorageLocation.FRIDGE
)

/**
 * Categories of food items
 */
enum class FoodCategory {
    DAIRY,
    MEAT,
    VEGETABLES,
    FRUITS,
    GRAINS,
    BEVERAGES,
    SNACKS,
    CONDIMENTS,
    OTHER
}

/**
 * Where the food is stored
 */
enum class StorageLocation {
    FRIDGE,
    FREEZER,
    PANTRY
}