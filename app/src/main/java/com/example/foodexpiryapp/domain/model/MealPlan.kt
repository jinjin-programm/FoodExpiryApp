package com.example.foodexpiryapp.domain.model

import java.time.LocalDate

data class MealPlan(
    val id: Long = 0,
    val date: LocalDate,
    val slot: MealSlot,
    val itemType: MealItemType,
    val recipeId: Long?,
    val recipeName: String?,
    val productName: String?,
    val inventoryItemId: Long?
) {
    val displayName: String
        get() = when (itemType) {
            MealItemType.RECIPE -> recipeName ?: ""
            MealItemType.PRODUCT -> productName ?: ""
        }
}
