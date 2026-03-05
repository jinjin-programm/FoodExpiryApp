package com.example.foodexpiryapp.presentation.ui.llm

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LlmVisionService(private val context: Context) {

    companion object {
        private const val TAG = "LlmVisionService"
        private const val MODEL_DIR = "llm"
        private const val MODEL_FILE = "model.gguf"
    }

    private var isInitialized = false
    private val llamaBridge = LlamaBridge.getInstance()

    data class DetectionResult(
        val foodName: String,
        val expiryDate: String?,
        val confidence: String,
        val rawResponse: String
    )

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelPath = getModelPath()
            val modelFile = File(modelPath, MODEL_FILE)
            
            if (!modelFile.exists()) {
                Log.w(TAG, "Model not found at: ${modelFile.absolutePath}")
                // Try assets directory
                val assetModel = File(context.filesDir, "models/$MODEL_FILE")
                if (!assetModel.exists()) {
                    Log.w(TAG, "Please copy Qwen3.5 GGUF model to: $modelPath/$MODEL_FILE")
                    // Initialize anyway - we have fallback
                    isInitialized = true
                    return@withContext true
                }
            }

            // Try to load the model
            val loaded = llamaBridge.loadModel(modelFile.absolutePath, 2048, 4)
            isInitialized = loaded
            
            Log.i(TAG, "LLM initialization: ${if (loaded) "success" else "using stub"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}", e)
            isInitialized = true // Allow stub mode
            true
        }
    }

    private fun getModelPath(): String {
        return File(context.filesDir, MODEL_DIR).absolutePath
    }

    suspend fun analyzeImage(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) {
                return@withContext DetectionResult(
                    foodName = "Not initialized",
                    expiryDate = null,
                    confidence = "low",
                    rawResponse = "LLM service not initialized"
                )
            }

            val prompt = buildPrompt()
            val base64Image = bitmapToBase64(bitmap)
            
            val fullPrompt = "$prompt\n\nImage (base64): $base64Image"
            
            // Try native inference if model is loaded
            val response = if (llamaBridge.isLoaded()) {
                llamaBridge.generate(fullPrompt)
            } else {
                // Fallback to color-based detection
                runFallbackDetection(bitmap)
            }

            parseResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            DetectionResult(
                foodName = "Error",
                expiryDate = null,
                confidence = "low",
                rawResponse = e.message ?: "Unknown error"
            )
        }
    }

    private fun runFallbackDetection(bitmap: Bitmap): String {
        val avgColor = getAverageColor(bitmap)
        val detectedFood = classifyByColor(avgColor)
        
        return """
1. Food item name: $detectedFood
2. Expiry date: not visible
3. Confidence: low
        """.trimIndent()
    }

    private fun getAverageColor(bitmap: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
        var r = 0
        var g = 0
        var b = 0
        var count = 0
        
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val pixel = scaled.getPixel(x, y)
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
                count++
            }
        }
        
        r /= count
        g /= count
        b /= count
        
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun classifyByColor(color: Int): String {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        
        return when {
            r > g && r > b && r > 150 -> {
                if (g < 100) "Apple/Red Fruit" else "Tomato/Strawberry"
            }
            r > 180 && g > 150 && b < 100 -> {
                if (g > 200) "Banana/Lemon" else "Orange/Carrot"
            }
            g > r && g > b && g > 120 -> {
                if (b < 80) "Lettuce/Cucumber" else "Broccoli/Spinach"
            }
            r in 100..180 && g in 80..150 && b < 100 -> {
                "Bread/Coffee/Meat"
            }
            r > 180 && g > 180 && b > 180 -> {
                "Rice/Milk/Cheese"
            }
            b > r && b > g -> {
                "Blueberry/Grape"
            }
            else -> "Unknown Food"
        }
    }

    private fun buildPrompt(): String {
        return """
Analyze this food image and respond with ONLY the following format (no other text):
1. Food item name: [name]
2. Expiry date: [date in DD/MM/YYYY format or "not visible"]
3. Confidence: [high/medium/low]
        """.trimIndent()
    }

    private fun parseResponse(response: String): DetectionResult {
        var foodName = "Unknown"
        var expiryDate: String? = null
        var confidence = "medium"

        val lines = response.split("\n")
        for (line in lines) {
            when {
                line.contains("Food item name:", ignoreCase = true) -> {
                    foodName = line.substringAfter(":").trim()
                        .replace("[", "").replace("]", "")
                }
                line.contains("Expiry date:", ignoreCase = true) -> {
                    val date = line.substringAfter(":").trim()
                        .replace("[", "").replace("]", "")
                    expiryDate = if (date.equals("not visible", ignoreCase = true)) null else date
                }
                line.contains("Confidence:", ignoreCase = true) -> {
                    confidence = line.substringAfter(":").trim()
                        .replace("[", "").replace("]", "")
                        .lowercase()
                }
            }
        }

        if (foodName == "Unknown" || foodName.isBlank()) {
            foodName = extractFoodFromRaw(response)
        }

        return DetectionResult(
            foodName = foodName,
            expiryDate = expiryDate,
            confidence = confidence,
            rawResponse = response
        )
    }

    private fun extractFoodFromRaw(response: String): String {
        val commonFoods = listOf(
            "apple", "banana", "orange", "milk", "egg", "bread", "cheese",
            "chicken", "beef", "pork", "fish", "rice", "pasta", "yogurt",
            "butter", "lettuce", "tomato", "potato", "onion", "carrot",
            "watermelon", "grape", "strawberry", "blueberry", "mango"
        )

        val lowerResponse = response.lowercase()
        for (food in commonFoods) {
            if (lowerResponse.contains(food)) {
                return food.replaceFirstChar { it.uppercase() }
            }
        }

        return "Unknown"
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resized = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
        
        val stream = java.io.ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val byteArray = stream.toByteArray()
        
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    fun release() {
        isInitialized = false
        try {
            llamaBridge.freeModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing: ${e.message}")
        }
        Log.i(TAG, "LLM service released")
    }
}
