package com.example.foodexpiryapp.inference.mnn

import android.util.Log
import com.example.foodexpiryapp.domain.model.FoodIdentification
import org.json.JSONObject

/**
 * Parses LLM response into FoodIdentification.
 * Per MNN-04: Extracts {"name", "name_zh"} from JSON output.
 * Handles malformed JSON gracefully.
 */
object StructuredOutputParser {
    private const val TAG = "StructuredOutputParser"

    /**
     * Parses raw LLM text response into FoodIdentification.
     * Handles various output formats:
     * 1. Clean JSON: {"name": "apple", "name_zh": "蘋果"}
     * 2. JSON in markdown: ```json\n{...}\n```
     * 3. JSON with preamble text: "Here is the food: {...}"
     * 4. Malformed JSON: best-effort extraction
     */
    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val cleaned = rawResponse.trim()

        // Try direct JSON parse
        try {
            return parseJson(cleaned)
        } catch (e: Exception) {
            Log.d(TAG, "Direct JSON parse failed, trying extraction")
        }

        // Extract JSON from markdown code block
        val jsonBlockRegex = Regex("""```(?:json)?\s*(\{[^`]*\})\s*```""")
        val blockMatch = jsonBlockRegex.find(cleaned)
        if (blockMatch != null) {
            try {
                return parseJson(blockMatch.groupValues[1])
            } catch (e: Exception) {
                Log.d(TAG, "Code block JSON parse failed")
            }
        }

        // Extract JSON object from surrounding text
        val jsonRegex = Regex("""\{[^{}]*"name"[^{}]*\}""")
        val jsonMatch = jsonRegex.find(cleaned)
        if (jsonMatch != null) {
            try {
                return parseJson(jsonMatch.value)
            } catch (e: Exception) {
                Log.d(TAG, "Extracted JSON parse failed")
            }
        }

        // Last resort: try to find name patterns
        return parseFallback(cleaned)
    }

    private fun parseJson(json: String): FoodIdentification {
        val obj = JSONObject(json)

        val name = obj.optString("name", "Unknown")
        val nameZh = obj.optString("name_zh", obj.optString("nameZh", ""))
        val confidence = obj.optDouble("confidence", 0.8).toFloat()
        val category = obj.optString("category", null)
        val expiryHint = obj.optString("expiry_hint", obj.optString("expiryHint", null))

        return FoodIdentification(
            name = name.ifBlank { "Unknown" },
            nameZh = nameZh.ifBlank { name },  // Fallback to English name
            confidence = confidence.coerceIn(0f, 1f),
            expiryHint = expiryHint.ifBlank { null },
            rawResponse = json
        )
    }

    private fun parseFallback(text: String): FoodIdentification? {
        // Try to extract "name": "..." and "name_zh": "..." patterns
        val nameMatch = Regex(""""name"\s*:\s*"([^"]+)"""").find(text)
        val nameZhMatch = Regex(""""name_zh"\s*:\s*"([^"]+)"""").find(text)

        if (nameMatch != null) {
            return FoodIdentification(
                name = nameMatch.groupValues[1],
                nameZh = nameZhMatch?.groupValues?.get(1) ?: nameMatch.groupValues[1],
                confidence = 0.5f,  // Lower confidence for fallback parse
                rawResponse = text
            )
        }

        Log.w(TAG, "Could not parse food identification from: ${text.take(100)}")
        return null
    }
}
