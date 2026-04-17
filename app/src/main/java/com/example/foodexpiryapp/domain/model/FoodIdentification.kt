package com.example.foodexpiryapp.domain.model

/**
 * Structured output from LLM food identification.
 * Per MNN-04: LLM returns JSON with {"name", "name_zh"}.
 *
 * This is a pure Kotlin data class with no Android dependencies,
 * following the same pattern as [FoodItem] in the domain model layer.
 */
data class FoodIdentification(
    val name: String,               // English food name from LLM
    val nameZh: String,             // Chinese food name (per MNN-04 requirement)
    val confidence: Float = 0.0f,   // Confidence score from model
    val expiryHint: String? = null, // Optional expiry date hint from LLM
    val shelfLifeDays: Int? = null,  // Shelf life in days from LLM
    val rawResponse: String? = null // Raw JSON for debugging
)
