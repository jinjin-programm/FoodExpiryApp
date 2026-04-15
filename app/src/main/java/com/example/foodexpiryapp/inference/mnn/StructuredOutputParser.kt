package com.example.foodexpiryapp.inference.mnn

import com.example.foodexpiryapp.domain.model.FoodIdentification

object StructuredOutputParser {
    private val CHAT_TEMPLATE_TOKEN_RE = Regex("""<\|im_start\|>|<\|im_end\|>|<\|endoftext\|>|<\|[^|]+\|>|```(?:json)?|```""")

    fun parse(rawResponse: String?): FoodIdentification? {
        if (rawResponse.isNullOrBlank()) return null

        val cleaned = rawResponse
            .replace(CHAT_TEMPLATE_TOKEN_RE, "")
            .trim()

        if (cleaned.isBlank()) return null

        val firstLine = cleaned.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            .orEmpty()

        val name = firstLine
            .removePrefix("-")
            .trim()

        return if (name.isNotBlank()) {
            FoodIdentification(
                name = name,
                nameZh = name,
                confidence = 1.0f,
                rawResponse = rawResponse
            )
        } else {
            null
        }
    }
}
