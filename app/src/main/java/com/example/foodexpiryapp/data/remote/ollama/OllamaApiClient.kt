package com.example.foodexpiryapp.data.remote.ollama

import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaChatRequest
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaChatResponse
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaModelsResponse
import com.example.foodexpiryapp.data.remote.ollama.dto.OllamaVersionResponse
import com.example.foodexpiryapp.util.AppLog
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
class OllamaApiClient @Inject constructor(
    private val serverConfig: OllamaServerConfig,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "OllamaApiClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun chat(request: OllamaChatRequest): OllamaChatResponse =
        withContext(Dispatchers.IO) {
            val config = serverConfig.getConfig()
            val url = "${config.baseUrl}/api/chat"
            val jsonBody = gson.toJson(request)

            AppLog.d(TAG, "REQUEST → ${request.model} @ ${config.baseUrl}")
            AppLog.d(TAG, "OPTIONS → temp=${request.options?.temperature} topP=${request.options?.topP} numPredict=${request.options?.numPredict}")
            AppLog.d(TAG, "PROMPT → ${request.messages.firstOrNull()?.content?.take(200)}")

            val startTime = System.currentTimeMillis()
            val requestBuilder = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))

            config.apiToken?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string()
            val elapsed = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                AppLog.e(TAG, "API ERROR ← ${response.code} (${elapsed}ms): $responseBody")
                throw Exception("API error ${response.code}: $responseBody")
            }

            AppLog.d(TAG, "RESPONSE ← ${response.code} in ${elapsed}ms")

            val parsed = gson.fromJson(responseBody, OllamaChatResponse::class.java)
                ?: throw Exception("Failed to parse response")

            AppLog.d(TAG, "MODEL → ${parsed.model}, TOKENS → eval_count=${parsed.evalCount} eval_duration=${parsed.evalDuration}ms")
            if (parsed.evalCount != null && parsed.evalDuration != null && parsed.evalDuration > 0) {
                val tokensPerSec = parsed.evalCount.toDouble() / (parsed.evalDuration.toDouble() / 1_000_000_000.0)
                AppLog.d(TAG, "SPEED → %.1f tokens/sec".format(tokensPerSec))
            }

            parsed
        }

    suspend fun getVersion(): OllamaVersionResponse = withContext(Dispatchers.IO) {
        val config = serverConfig.getConfig()
        val url = "${config.baseUrl}/api/version"

        val requestBuilder = Request.Builder().url(url).get()

        config.apiToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        gson.fromJson(responseBody, OllamaVersionResponse::class.java)
            ?: throw Exception("Failed to parse version response")
    }

    suspend fun getModels(): OllamaModelsResponse = withContext(Dispatchers.IO) {
        val config = serverConfig.getConfig()
        val url = "${config.baseUrl}/api/tags"

        val requestBuilder = Request.Builder().url(url).get()

        config.apiToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = httpClient.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        gson.fromJson(responseBody, OllamaModelsResponse::class.java)
            ?: throw Exception("Failed to parse models response")
    }
}
