package com.example.foodexpiryapp.inference.mnn

import android.util.Log
import com.example.foodexpiryapp.domain.model.FoodIdentification
import org.json.JSONObject

object StructuredOutputParser {
    private const val TAG = "StructuredOutputParser"

    private val JSON_BLOCK_RE = Regex("""\{[^{}]*\}""")

    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val cleaned = rawResponse.trim()

        val json = extractJson(cleaned)
        if (json != null) {
            try {
                val result = parseJson(json)
                Log.d(TAG, "Parsed JSON food: '${result.name}' / '${result.nameZh}'")
                return result
            } catch (e: Exception) {
                Log.w(TAG, "JSON matched but failed to parse: $json", e)
            }
        }

        return parsePlainText(cleaned)
    }

    private fun extractJson(text: String): String? {
        val candidates = JSON_BLOCK_RE.findAll(text).map { it.value }.toList()

        for (candidate in candidates) {
            if (!isValidFoodJson(candidate)) continue
            return candidate
        }

        return null
    }

    private fun isValidFoodJson(json: String): Boolean {
        if (json.length < 10) return false
        if (json.contains("...")) return false
        if (!json.contains("\"name\"") && !json.contains("\"name_en\"")) return false

        return try {
            val obj = JSONObject(json)
            val name = obj.optString("name", "")
            val nameEn = obj.optString("name_en", "")
            val hasName = name.isNotBlank() && name != "..." && name != "food_name"
            val hasNameEn = nameEn.isNotBlank() && nameEn != "..." && nameEn != "food_name"
            hasName || hasNameEn
        } catch (_: Exception) {
            false
        }
    }

    private fun parseJson(json: String): FoodIdentification {
        val obj = JSONObject(json)

        val nameEn = obj.optString("name_en", "")
        val name = if (nameEn.isNotBlank()) nameEn else obj.optString("name", "Unknown")

        val nameZh = obj.optString("name_zh", obj.optString("nameZh", ""))
        val category = obj.optString("category", null)
        val expiryHint = obj.optString("expiry_hint", obj.optString("expiryHint", null))

        return FoodIdentification(
            name = name.ifBlank { "Unknown" },
            nameZh = nameZh.ifBlank { name },
            confidence = 1.0f,
            expiryHint = expiryHint.ifBlank { null },
            rawResponse = json
        )
    }

    private fun parsePlainText(text: String): FoodIdentification? {
        val lines = text.lines().filter { it.isNotBlank() }
        for (line in lines.reversed()) {
            val trimmed = line.trim()
            if (trimmed.length in 2..60 && !trimmed.contains(" ") && !trimmed.contains(".")) {
                val name = capitalize(trimmed)
                if (name.lowercase() !in STOP_WORDS) {
                    Log.d(TAG, "Parsed plain text food: '$name' from: '$trimmed'")
                    return FoodIdentification(
                        name = name,
                        nameZh = name,
                        confidence = 1.0f,
                        rawResponse = text
                    )
                }
            }
        }

        Log.w(TAG, "Could not parse food from response: '$text'")
        return null
    }

    private fun capitalize(s: String): String =
        s.lowercase().replaceFirstChar { it.uppercase() }

    private val STOP_WORDS = setOf(
        "the", "a", "an", "this", "that", "it", "is", "are", "was", "were",
        "in", "on", "at", "to", "of", "for", "with", "from", "by",
        "i", "you", "he", "she", "we", "they", "my", "your", "his", "her",
        "image", "picture", "photo", "photograph", "appears",
        "looks", "like", "seems", "would", "could", "should", "can",
        "not", "no", "yes", "and", "or", "but", "if", "then",
        "unable", "sorry", "cannot", "don't", "doesn't", "isn't",
        "identify", "determine", "tell", "see", "observe", "recognize",
        "unknown", "none", "nothing", "error", "analyze", "zoom", "check",
        "final", "formulate", "determine"
    )
}
