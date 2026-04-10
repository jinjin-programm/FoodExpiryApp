package com.example.foodexpiryapp.inference.mnn

import android.util.Log
import com.example.foodexpiryapp.domain.model.FoodIdentification

object StructuredOutputParser {
    private const val TAG = "StructuredOutputParser"

    private val FOOD_TAG_REGEX = Regex("""\[FOOD\](.+?)\[/FOOD]""", RegexOption.IGNORE_CASE)

    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val match = FOOD_TAG_REGEX.findAll(rawResponse).lastOrNull {
        if (match != null) {
            val foodName = match.groupValues[1].trim()
            if (foodName.isNotBlank() && foodName.lowercase() != "unknown") {
                Log.d(TAG, "Parsed food: '$foodName'")
                return FoodIdentification(
                    name = foodName,
                    nameZh = foodName,
                    confidence = 1.0f,
                    rawResponse = rawResponse
                )
            }
            Log.d(TAG, "Parsed as Unknown food")
            return FoodIdentification(
                name = "Unknown",
                nameZh = "Unknown",
                confidence = 0.0f,
                rawResponse = rawResponse
            )
        }

        Log.w(TAG, "No [FOOD] tag found in response")
        return null
    }
}
