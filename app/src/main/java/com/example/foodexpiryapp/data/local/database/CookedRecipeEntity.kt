package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cooked_recipes")
data class CookedRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val recipeName: String,
    val cookedAt: Long = System.currentTimeMillis(),
    val moneySaved: Double,
    val wasteRescuedPercent: Int,
    val matchedIngredients: String,
    val imageUrl: String? = null
)