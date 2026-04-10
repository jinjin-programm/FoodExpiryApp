package com.example.foodexpiryapp.inference.mnn

import android.util.Log
import com.example.foodexpiryapp.domain.model.FoodIdentification

object StructuredOutputParser {
    private const val TAG = "StructuredOutputParser"

    private val THINKING_STEP_RE = Regex("""^\d+\.\s+\*\*\*""")

    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val lines = rawResponse.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines.reversed()) {
            if (THINKING_STEP_RE.containsMatchIn(line)) continue
            if (line.length < 2 || line.length > 60) continue
            val name = line.replace(Regex("""^[`\"]+"""), "").replace(Regex("""[`"]+$"""), "")
                .trim()
            if (name.isNotBlank()) {
                Log.d(TAG, "Parsed food: '$name'")
                return FoodIdentification(
                    name = name,
                    nameZh = name,
                    confidence = 1.0f,
                    rawResponse = rawResponse
                )
            }
        }

        Log.w(TAG, "No food name found in response")
        return null
    }
}
