package com.example.foodexpiryapp.inference.mnn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StructuredOutputParserTest {

    @Test
    fun parsesPlainTextFoodName() {
        val result = StructuredOutputParser.parse("Strawberry")

        assertNotNull(result)
        assertEquals("Strawberry", result?.name)
        assertEquals("Strawberry", result?.nameZh)
    }
}
