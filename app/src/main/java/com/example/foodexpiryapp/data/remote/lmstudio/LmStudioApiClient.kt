package com.example.foodexpiryapp.data.remote.lmstudio

import android.util.Log
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiChatRequest
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiChatResponse
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiModelsResponse
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiTextContent
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiImageContent
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiImageUrl
import com.example.foodexpiryapp.data.remote.lmstudio.dto.OpenAiMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LmStudioApiClient @Inject constructor(
    private val serverConfig: LmStudioServerConfig,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "LmStudioApiClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun chat(request: OpenAiChatRequest): OpenAiChatResponse =
        withContext(Dispatchers.IO) {
            val config = serverConfig.getConfig()
            val url = "${config.baseUrl}/v1/chat/completions"
            val jsonBody = gson.toJson(request)

            Log.d(TAG, "─────────────────────────────────")
            Log.d(TAG, "REQUEST → ${request.model} @ ${config.baseUrl}")
            Log.d(TAG, "OPTIONS → temp=${request.temperature} topP=${request.topP} maxTokens=${request.maxTokens}")

            val startTime = System.currentTimeMillis()
            val requestBuilder = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string()
            val elapsed = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                Log.e(TAG, "API ERROR ← ${response.code} (${elapsed}ms): $responseBody")
                throw Exception("API error ${response.code}: $responseBody")
            }

            Log.d(TAG, "RESPONSE ← ${response.code} in ${elapsed}ms")
            Log.d(TAG, "RAW BODY → $responseBody")

            val parsed = gson.fromJson(responseBody, OpenAiChatResponse::class.java)
                ?: throw Exception("Failed to parse response")

            val content = parsed.choices?.firstOrNull()?.message?.content
            Log.d(TAG, "CONTENT → $content")
            Log.d(TAG, "TOKENS → prompt=${parsed.usage?.promptTokens} completion=${parsed.usage?.completionTokens} total=${parsed.usage?.totalTokens}")
            if (parsed.usage?.completionTokens != null && elapsed > 0) {
                val tokensPerSec = parsed.usage.completionTokens.toDouble() / (elapsed.toDouble() / 1000.0)
                Log.d(TAG, "SPEED → %.1f tokens/sec".format(tokensPerSec))
            }
            Log.d(TAG, "─────────────────────────────────")

            parsed
        }

    suspend fun getModels(): OpenAiModelsResponse = withContext(Dispatchers.IO) {
        val config = serverConfig.getConfig()
        val url = "${config.baseUrl}/v1/models"

        val requestBuilder = Request.Builder().url(url).get()

        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        gson.fromJson(responseBody, OpenAiModelsResponse::class.java)
            ?: throw Exception("Failed to parse models response")
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val config = serverConfig.getConfig()
            val url = "${config.baseUrl}/v1/models"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            false
        }
    }
}
