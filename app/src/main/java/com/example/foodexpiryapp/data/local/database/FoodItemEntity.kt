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
    val category: String,       // stored as enum name string
    val expiryDate: String,     // stored as ISO-8601 (yyyy-MM-dd)
    val quantity: Int = 1,
    val location: String,       // stored as enum name string
    val notes: String = "",
    val barcode: String? = null,
    val dateAdded: String       // ISO-8601 date when item was added
)
