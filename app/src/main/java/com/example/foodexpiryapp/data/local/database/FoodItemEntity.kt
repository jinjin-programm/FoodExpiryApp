package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.StorageLocation

/**
 * Room entity representing a food item stored in the local database.
 */
@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val expiryDate: String,
    val quantity: Int = 1,
    val location: String,
    val notes: String = "",
    val barcode: String? = null,
    val dateAdded: String,
    val notifyEnabled: Boolean = true,
    val notifyDaysBefore: Int? = null,
    val purchaseDate: String? = null,
    val scanSource: String = "MANUAL",
    val confidence: Float = 1.0f,
    val riskLevel: String = "LOW",
    val recipeRelevance: Float = 0.0f
)
