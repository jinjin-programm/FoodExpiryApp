package com.example.foodexpiryapp.data.remote.ollama

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaChatRequest
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaJsonSchema
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaMessage
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaOptions
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaSchemaProperty
import com.example.foodexpiryapp.domain.model.FoodIdentification
import com.example.foodexpiryapp.domain.client.FoodVisionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaVisionClient @Inject constructor(
    private val apiClient: OllamaApiClient,
    private val serverConfig: OllamaServerConfig
) : FoodVisionClient {
    companion object {
        private const val TAG = "OllamaVisionClient"
        private const val MAX_IMAGE_SIZE = 512
        private const val JPEG_QUALITY = 85
    }

    override suspend fun analyzeFood(
        bitmap: Bitmap,
        hint: String?
    ): FoodIdentification? = withContext(Dispatchers.IO) {
        try {
            val config = serverConfig.getConfig()
            val base64Image = bitmapToBase64(bitmap)

            val prompt = buildString {
                append("Identify the food in this image.")
                if (!hint.isNullOrBlank()) {
                    append(" Additional context: $hint")
                }
                append(" Respond with JSON containing: name (English), name_zh (Chinese), confidence (0-1), expiry_hint (optional).")
            }

            val messages = listOf(
                OllamaMessage(
                    role = "user",
                    content = prompt,
                    images = listOf(base64Image)
                )
            )

            val jsonSchema = OllamaJsonSchema(
                type = "object",
                properties = mapOf(
                    "name" to OllamaSchemaProperty(type = "string", description = "English name of the food"),
                    "name_zh" to OllamaSchemaProperty(type = "string", description = "Chinese name of the food"),
                    "confidence" to OllamaSchemaProperty(type = "number", description = "Confidence score between 0 and 1"),
                    "expiry_hint" to OllamaSchemaProperty(type = "string", description = "Expiry date hint if visible")
                ),
                required = listOf("name", "name_zh", "confidence")
            )

            val options = OllamaOptions(
                temperature = 0.1f,
                topP = 0.9f,
                topK = 40,
                numPredict = 100,
                seed = 42
            )

            val request = OllamaChatRequest(
                model = config.modelName,
                messages = messages,
                stream = false,
                format = jsonSchema,
                options = options,
                keepAlive = "5m"
            )

            val response = apiClient.chat(request)

            if (response.message != null) {
                parseFoodIdentification(response.message.content)
            } else {
                Log.e(TAG, "Empty response message")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing food", e)
            null
        }
    }

    override suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            apiClient.getVersion()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resizedBitmap = resizeBitmapIfNeeded(bitmap)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }

        val scale = MAX_IMAGE_SIZE.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun parseFoodIdentification(jsonContent: String): FoodIdentification? {
        return try {
            val cleaned = jsonContent
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = if (cleaned.startsWith("[")) {
                val arr = org.json.JSONArray(cleaned)
                if (arr.length() > 0) arr.getJSONObject(0) else throw Exception("Empty array")
            } else {
                org.json.JSONObject(cleaned)
            }

            val name = json.optString("name", "").trim()
            val nameZh = json.optString("name_zh", "").trim()
            val confidence = json.optDouble("confidence", 0.0).toFloat()
            val expiryHint = json.optString("expiry_hint", null)?.takeIf { it.isNotBlank() }

            if (name.isBlank() && nameZh.isBlank()) {
                Log.w(TAG, "Empty food name in response")
                return null
            }

            FoodIdentification(
                name = name.ifBlank { nameZh },
                nameZh = nameZh.ifBlank { name },
                confidence = confidence.coerceIn(0f, 1f),
                expiryHint = expiryHint,
                rawResponse = jsonContent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response", e)
            null
        }
    }
}