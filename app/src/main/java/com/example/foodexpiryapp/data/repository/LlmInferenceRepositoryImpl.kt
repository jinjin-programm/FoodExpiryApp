package com.example.foodexpiryapp.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.foodexpiryapp.data.remote.ProviderConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioVisionClient
import com.example.foodexpiryapp.data.remote.ollama.OllamaVisionClient
import com.example.foodexpiryapp.domain.client.FoodVisionClient
import com.example.foodexpiryapp.domain.model.FoodIdentification
import com.example.foodexpiryapp.domain.model.ModelState
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmInferenceRepositoryImpl @Inject constructor(
    private val ollamaClient: OllamaVisionClient,
    private val lmStudioClient: LmStudioVisionClient,
    private val providerConfig: ProviderConfig
) : LlmInferenceRepository {

    companion object {
        private const val TAG = "LlmInferenceRepo"
    }

    private val inferenceMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotDownloaded)

    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private suspend fun getActiveClient(): FoodVisionClient {
        val provider = providerConfig.getProvider()
        return if (provider == ProviderConfig.PROVIDER_LMSTUDIO) lmStudioClient else ollamaClient
    }

    init {
        scope.launch { checkServerConnection() }
    }

    override fun inferFood(bitmap: Bitmap): Flow<FoodIdentification> = flow {
        inferenceMutex.withLock {
            try {
                val client = getActiveClient()
                val provider = providerConfig.getProvider()
                val providerName = if (provider == ProviderConfig.PROVIDER_LMSTUDIO) "LM Studio" else "Ollama"

                if (!client.testConnection()) {
                    Log.w(TAG, "$providerName server not connected")
                    _modelState.value = ModelState.Error("無法連接${providerName}服務器")
                    emit(
                        FoodIdentification(
                            name = "Error",
                            nameZh = "${providerName}服務器未連接",
                            confidence = 0f,
                            rawResponse = "無法連接到${providerName}服務器。請檢查設置。"
                        )
                    )
                    return@flow
                }

                _modelState.value = ModelState.Loading

                Log.d(TAG, "Running food analysis on ${bitmap.width}x${bitmap.height} bitmap via $providerName")
                val result = client.analyzeFood(bitmap)

                if (result != null) {
                    Log.d(TAG, "Analysis result: ${result.name} / ${result.nameZh} (confidence: ${result.confidence})")
                    _modelState.value = ModelState.Ready
                    emit(result)
                } else {
                    Log.w(TAG, "Analysis returned null")
                    _modelState.value = ModelState.Error("識別失敗")
                    emit(
                        FoodIdentification(
                            name = "Unknown",
                            nameZh = "無法識別",
                            confidence = 0f,
                            rawResponse = "無法識別圖片中的食物。"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error", e)
                _modelState.value = ModelState.Error(e.message ?: "未知錯誤")
                emit(
                    FoodIdentification(
                        name = "Error",
                        nameZh = "發生錯誤",
                        confidence = 0f,
                        rawResponse = "錯誤: ${e.message}"
                    )
                )
            }
        }
    }

    suspend fun checkServerConnection(): Boolean {
        return try {
            val client = getActiveClient()
            val isConnected = client.testConnection()
            _modelState.value = if (isConnected) {
                ModelState.Ready
            } else {
                ModelState.Error("無法連接服務器")
            }
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Connection check failed", e)
            _modelState.value = ModelState.Error("連接錯誤: ${e.message}")
            false
        }
    }
}
