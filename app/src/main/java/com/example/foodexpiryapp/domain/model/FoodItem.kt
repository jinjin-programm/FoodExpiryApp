package com.example.foodexpiryapp.domain.model

import java.time.LocalDate

/**
 * Domain model representing a food item in the user's pantry.
 * This is a pure Kotlin class with no Android/Room dependencies.
 */
data class FoodItem(
    val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val expiryDate: LocalDate,
    val quantity: Int = 1,
    val location: StorageLocation = StorageLocation.FRIDGE,
    val notes: String = "",
    val barcode: String? = null,
    val dateAdded: LocalDate = LocalDate.now(),
    val notifyEnabled: Boolean = true,
    val notifyDaysBefore: Int? = null,
    val purchaseDate: LocalDate? = null,
    val scanSource: ScanSource = ScanSource.MANUAL,
    val confidence: Float = 1.0f,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val recipeRelevance: Float = 0.0f
) {
    /** True if this item has already expired. */
    val isExpired: Boolean
        get() = expiryDate.isBefore(LocalDate.now())

    /** Number of days until expiry (negative if already expired). */
    val daysUntilExpiry: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate)
}

/**
 * Categories of food items.
 */
enum class FoodCategory(val displayName: String) {
    DAIRY("Dairy"),
    MEAT("Meat"),
    VEGETABLES("Vegetables"),
    FRUITS("Fruits"),
    GRAINS("Grains"),
    BEVERAGES("Beverages"),
    SNACKS("Snacks"),
    CONDIMENTS("Condiments"),
    FROZEN("Frozen"),
    LEFTOVERS("Leftovers"),
    OTHER("Other")
}

/**
 * Where the food is stored.
 */
enum class StorageLocation(val displayName: String) {
    FRIDGE("Fridge"),
    FREEZER("Freezer"),
    PANTRY("Pantry"),
    COUNTER("Counter")
}

/**
 * Source from which the food item was added.
 */
enum class ScanSource {
    MANUAL,
    BARCODE,
    RECEIPT,
    INGREDIENT_LIST
}

/**
 * Risk level associated with the food item (e.g. high risk for meat).
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
