package com.example.foodexpiryapp.inference.mnn

import com.example.foodexpiryapp.domain.model.FoodIdentification

object StructuredOutputParser {
    private val CHAT_TEMPLATE_TOKEN_RE = Regex("""<\|im_start\|>|<\|im_end\|>|<\|endoftext\|>|<\|[^|]+\|>|```(?:json)?|```""")
    private val FOOD_IS_RE = Regex("""(?:the\s+)?food\s+is\s+([a-zA-Z\s]+?)(?:\.|\n|$)""", RegexOption.IGNORE_CASE)
    private val STEP3_RE = Regex("""STEP\s*3:.*?(?:The\s+food\s+is\s+)?([a-zA-Z]+)(?:\.|\n|$)""", RegexOption.IGNORE_CASE)
    private val STOP_WORDS = setOf("the", "a", "an", "is", "this", "image", "shows", "showing", "food", "not", "yes", "no", "step")

    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val cleaned = rawResponse
            .replace(CHAT_TEMPLATE_TOKEN_RE, "")
            .trim()

        if (cleaned.isBlank()) return null

        FOOD_IS_RE.find(cleaned)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank() && name.length > 1) {
                return FoodIdentification(
                    name = name,
                    nameZh = name,
                    confidence = 1.0f,
                    rawResponse = rawResponse
                )
            }
        }

        STEP3_RE.find(cleaned)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank() && name.length > 1) {
                return FoodIdentification(
                    name = name,
                    nameZh = name,
                    confidence = 1.0f,
                    rawResponse = rawResponse
                )
            }
        }

        val lines = cleaned.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (line in lines) {
            val candidate = line
                .removePrefix("-")
                .removePrefix("*")
                .trim()
                .removeSuffix(".")
                .removeSuffix("!")
                .trim()

            if (candidate.isNotBlank() && candidate.length > 1 && candidate.length < 40) {
                val words = candidate.lowercase().split(Regex("\\s+"))
                if (words.any { it !in STOP_WORDS }) {
                    return FoodIdentification(
                        name = candidate,
                        nameZh = candidate,
                        confidence = 0.8f,
                        rawResponse = rawResponse
                    )
                }
            }
        }

        return null
    }
}
