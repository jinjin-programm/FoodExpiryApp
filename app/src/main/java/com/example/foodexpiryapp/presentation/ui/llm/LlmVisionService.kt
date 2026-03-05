package com.example.foodexpiryapp.presentation.ui.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class LlmVisionService(private val context: Context) {

    companion object {
        private const val TAG = "LlmVisionService"
        private const val MODEL_DIR = "llm"
        private const val MODEL_FILE = "model.gguf"
        
        // Food color signatures (HSV ranges)
        private val FOOD_COLORS = mapOf(
            // Reds
            "Apple" to listOf(floatArrayOf(0f, 10f), floatArrayOf(350f, 360f)),
            "Tomato" to listOf(floatArrayOf(0f, 15f)),
            "Strawberry" to listOf(floatArrayOf(0f, 20f)),
            "Cherry" to listOf(floatArrayOf(0f, 15f)),
            "Watermelon" to listOf(floatArrayOf(0f, 20f, 100f, 120f)),
            
            // Oranges/Yellows  
            "Orange" to listOf(floatArrayOf(20f, 45f)),
            "Carrot" to listOf(floatArrayOf(15f, 40f)),
            "Banana" to listOf(floatArrayOf(40f, 60f)),
            "Lemon" to listOf(floatArrayOf(50f, 65f)),
            "Mango" to listOf(floatArrayOf(30f, 55f)),
            "Pineapple" to listOf(floatArrayOf(45f, 65f)),
            
            // Greens
            "Lettuce" to listOf(floatArrayOf(80f, 140f)),
            "Cucumber" to listOf(floatArrayOf(90f, 150f)),
            "Broccoli" to listOf(floatArrayOf(90f, 140f)),
            "Spinach" to listOf(floatArrayOf(80f, 160f)),
            "Avocado" to listOf(floatArrayOf(70f, 130f)),
            "Grape" to listOf(floatArrayOf(100f, 160f, 270f, 320f)),
            
            // Blues/Purples
            "Blueberry" to listOf(floatArrayOf(230f, 270f)),
            "Plum" to listOf(floatArrayOf(280f, 320f)),
            "Eggplant" to listOf(floatArrayOf(270f, 310f)),
            
            // Browns/Tans
            "Bread" to listOf(floatArrayOf(25f, 45f)),
            "Potato" to listOf(floatArrayOf(30f, 50f)),
            "Chocolate" to listOf(floatArrayOf(20f, 40f)),
            "Coffee beans" to listOf(floatArrayOf(10f, 35f)),
            
            // Whites/Creams
            "Rice" to listOf(floatArrayOf(0f, 360f)),
            "Milk" to listOf(floatArrayOf(0f, 360f)),
            "Cheese" to listOf(floatArrayOf(40f, 70f)),
            "Yogurt" to listOf(floatArrayOf(0f, 50f)),
            "Egg" to listOf(floatArrayOf(0f, 60f)),
            
            // Pinks
            "Salmon" to listOf(floatArrayOf(350f, 20f)),
            "Ham" to listOf(floatArrayOf(340f, 20f)),
            "Shrimp" to listOf(floatArrayOf(350f, 30f))
        )
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
            
            // Try to copy from assets if not exists
            if (!modelFile.exists()) {
                try {
                    context.assets.open("llm/$MODEL_FILE").use { input ->
                        modelFile.parentFile?.mkdirs()
                        modelFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.i(TAG, "Model copied from assets")
                } catch (e: Exception) {
                    Log.w(TAG, "Model not in assets, using color detection fallback: ${e.message}")
                }
            }
            
            // Try to load LLM (will fail gracefully)
            if (modelFile.exists()) {
                val loaded = llamaBridge.loadModel(modelFile.absolutePath, 2048, 4)
                isInitialized = loaded
                Log.i(TAG, "LLM loaded: $loaded")
            } else {
                isInitialized = true // Allow fallback
                Log.i(TAG, "Using color detection fallback")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Init error: ${e.message}")
            isInitialized = true
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
                    rawResponse = "Service not ready"
                )
            }

            // Try LLM if model loaded
            if (llamaBridge.isLoaded()) {
                val prompt = buildPrompt()
                val base64Image = bitmapToBase64(bitmap)
                val fullPrompt = "$prompt\n\nImage: $base64Image"
                
                try {
                    val response = llamaBridge.generate(fullPrompt)
                    return@withContext parseResponse(response)
                } catch (e: Exception) {
                    Log.w(TAG, "LLM failed: ${e.message}, using fallback")
                }
            }
            
            // Fallback to color detection
            val detectedFoods = detectFoodByColor(bitmap)
            val primaryFood = detectedFoods.firstOrNull() ?: "Unknown Food"
            
            DetectionResult(
                foodName = primaryFood,
                expiryDate = null,
                confidence = if (detectedFoods.size > 1) "medium" else "low",
                rawResponse = "Detected by color analysis: ${detectedFoods.joinToString(", ")}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Analysis error: ${e.message}")
            DetectionResult(
                foodName = "Error",
                expiryDate = null,
                confidence = "low",
                rawResponse = e.message ?: "Unknown error"
            )
        }
    }

    private fun detectFoodByColor(bitmap: Bitmap): List<String> {
        val scaled = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
        val histogram = mutableMapOf<String, Int>()
        
        // Sample pixels and build color histogram
        for (x in 0 until scaled.width step 5) {
            for (y in 0 until scaled.height step 5) {
                val pixel = scaled.getPixel(x, y)
                val hsv = FloatArray(3)
                Color.colorToHSV(pixel, hsv)
                
                val food = identifyFoodByHSV(hsv[0], hsv[1], hsv[2])
                if (food != null) {
                    histogram[food] = (histogram[food] ?: 0) + 1
                }
            }
        }
        
        // Return most detected foods
        return histogram.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    private fun identifyFoodByHSV(h: Float, s: Float, v: Float): String? {
        // Skip very dark or very light pixels
        if (v < 0.1f) return null
        if (v > 0.95f && s < 0.1f) return "Rice/Milk"
        
        // Check each food's color range
        for ((food, ranges) in FOOD_COLORS) {
            for (range in ranges) {
                when (range.size) {
                    2 -> {
                        // Single H range
                        if (h >= range[0] && h <= range[1] && s > 0.3f && v > 0.3f) {
                            return food
                        }
                    }
                    4 -> {
                        // Two H ranges (e.g., for watermelon: red rind + red flesh)
                        if ((h >= range[0] && h <= range[1]) || (h >= range[2] && h <= range[3])) {
                            if (s > 0.3f && v > 0.3f) return food
                        }
                    }
                }
            }
        }
        
        return null
    }

    private fun buildPrompt(): String {
        return """
Analyze this food image and respond with ONLY the following format:
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
            "watermelon", "grape", "strawberry", "blueberry", "mango",
            "pineapple", "cucumber", "broccoli", "spinach", "meat",
            "juice", "water", "soda", "coffee", "tea", "cookie", "cake"
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
        
        val stream = ByteArrayOutputStream()
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