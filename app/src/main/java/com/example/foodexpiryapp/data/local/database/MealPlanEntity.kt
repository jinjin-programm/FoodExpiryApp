package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val slot: String,
    val itemType: String,
    val recipeId: Long?,
    val recipeName: String?,
    val productName: String?,
    val inventoryItemId: Long?
)
