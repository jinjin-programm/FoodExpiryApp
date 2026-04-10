package com.example.foodexpiryapp.inference.mnn

import android.util.Log
import com.example.foodexpiryapp.domain.model.FoodIdentification
import org.json.JSONObject

object StructuredOutputParser {
    private const val TAG = "StructuredOutputParser"

    private val FOOD_PATTERNS = listOf(
        Regex("""The food is\s+(?:\[)?([^\]\.]+)""", RegexOption.IGNORE_CASE),
        Regex("""(?:it's|it is|this is|that is|this appears to be|this looks like)\s+(?:a\s+|an\s+)?(.+?)(?:\.|$)""", RegexOption.IGNORE_CASE),
        Regex("""(?:identified as|recognized as|appears to be)\s+(?:a\s+|an\s+)?(.+?)(?:\.|$)""", RegexOption.IGNORE_CASE),
        Regex("""(?:I (?:can |am able to )?see|I (?:can |am able to )?identify|I (?:can |am able to )?observe)\s+(?:a\s+|an\s+)?(.+?)(?:\s+in\s|(?:\.|$))""", RegexOption.IGNORE_CASE),
        Regex("""(?:there is|there's)\s+(?:a\s+|an\s+)?(.+?)(?:\s+in\s|(?:\.|$))""", RegexOption.IGNORE_CASE),
        Regex("""(?:the\s+(?:food|item|object|fruit|vegetable|meat)\s+(?:in\s+the\s+image\s+)?(?:is|appears to be))\s+(?:a\s+|an\s+)?(.+?)(?:\.|$)""", RegexOption.IGNORE_CASE)
    )

    private val STOP_WORDS = setOf(
        "the", "a", "an", "this", "that", "it", "is", "are", "was", "were",
        "in", "on", "at", "to", "of", "for", "with", "from", "by",
        "i", "you", "he", "she", "we", "they", "my", "your", "his", "her",
        "image", "picture", "photo", "photograph", "photo's", "appears",
        "looks", "like", "seems", "would", "could", "should", "can",
        "not", "no", "yes", "and", "or", "but", "if", "then",
        "unable", "sorry", "cannot", "don't", "doesn't", "isn't",
        "identify", "determine", "tell", "see", "observe", "recognize"
    )

    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val cleaned = rawResponse.trim()

        try {
            return parseJson(cleaned)
        } catch (_: Exception) {}

        val jsonBlockRegex = Regex("""```(?:json)?\s*(\{[^`]*\})\s*```""")
        val blockMatch = jsonBlockRegex.find(cleaned)
        if (blockMatch != null) {
            try {
                return parseJson(blockMatch.groupValues[1])
            } catch (_: Exception) {}
        }

        val jsonRegex = Regex("""\{[^{}]*"name_en"[^{}]*\}""")
        val jsonMatch = jsonRegex.find(cleaned)
        if (jsonMatch != null) {
            try {
                return parseJson(jsonMatch.value)
            } catch (_: Exception) {}
        }

        val jsonRegex2 = Regex("""\{[^{}]*"name"[^{}]*\}""")
        val jsonMatch2 = jsonRegex2.find(cleaned)
        if (jsonMatch2 != null) {
            try {
                return parseJson(jsonMatch2.value)
            } catch (_: Exception) {}
        }

        return parsePlainText(cleaned)
    }

    private fun parseJson(json: String): FoodIdentification {
        val obj = JSONObject(json)

        val nameEn = obj.optString("name_en", "")
        val name = if (nameEn.isNotBlank()) nameEn else obj.optString("name", "Unknown")

        val nameZh = obj.optString("name_zh", obj.optString("nameZh", ""))
        val confidence = 1.0f
        val category = obj.optString("category", null)
        val expiryHint = obj.optString("expiry_hint", obj.optString("expiryHint", null))

        return FoodIdentification(
            name = name.ifBlank { "Unknown" },
            nameZh = nameZh.ifBlank { name },
            confidence = confidence.coerceIn(0f, 1f),
            expiryHint = expiryHint.ifBlank { null },
            rawResponse = json
        )
    }

    private fun parsePlainText(text: String): FoodIdentification? {
        var extractedName = ""

        for (pattern in FOOD_PATTERNS) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val candidate = match.groupValues[1].trim()
                if (candidate.isNotBlank() && candidate.length > 1) {
                    extractedName = candidate
                    break
                }
            }
        }

        if (extractedName.isBlank()) {
            val lines = text.lines().filter { it.isNotBlank() }
            val lastLine = lines.lastOrNull()?.trim() ?: return null

            if (lastLine.length in 2..60 && !lastLine.contains(" ")) {
                extractedName = lastLine
            } else {
                val words = lastLine.split(Regex("\\s+"))
                val meaningfulWords = words.filter { word ->
                    word.lowercase() !in STOP_WORDS && word.length > 1
                }
                if (meaningfulWords.isNotEmpty()) {
                    extractedName = meaningfulWords.joinToString(" ")
                }
            }
        }

        if (extractedName.isBlank()) return null

        extractedName = extractedName
            .replace(Regex("""^[\s`"'\[(]+"""), "")
            .replace(Regex("""[\s`"'\])]+$"""), "")
            .replace(Regex("""[.!?;,]+$"""), "")
            .trim()

        if (extractedName.isBlank()) return null

        val upper = extractedName.uppercase()
        val isUnknown = upper == "UNKNOWN" ||
                upper == "NONE" ||
                upper == "NOTHING" ||
                upper == "NO FOOD" ||
                upper == "ERROR" ||
                upper.contains("UNABLE TO") ||
                upper.contains("CANNOT") ||
                upper.contains("CAN'T") ||
                upper.contains("DON'T") ||
                upper.contains("SORRY") ||
                upper.contains("I'M NOT") ||
                upper.contains("NO OBJECT")

        val name = if (isUnknown) "Unknown" else extractedName

        Log.d(TAG, "Parsed food name: '$name' from: '$text'")

        return FoodIdentification(
            name = name,
            nameZh = name,
            confidence = if (isUnknown) 0.0f else 1.0f,
            rawResponse = text
        )
    }
}
