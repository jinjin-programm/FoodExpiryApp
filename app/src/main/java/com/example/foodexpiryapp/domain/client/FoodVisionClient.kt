package com.example.foodexpiryapp.domain.client

import android.graphics.Bitmap
import com.example.foodexpiryapp.domain.model.FoodIdentification

interface FoodVisionClient {
    suspend fun analyzeFood(bitmap: Bitmap, hint: String? = null): FoodIdentification?
    suspend fun testConnection(): Boolean
}
