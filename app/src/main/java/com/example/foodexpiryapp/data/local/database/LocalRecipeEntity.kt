package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_recipes")
data class LocalRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val ingredients: String,
    val steps: String,
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val servings: Int = 2,
    val cuisine: String = "",
    val tags: String = "",
    val estimatedCost: Double = 0.0,
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
