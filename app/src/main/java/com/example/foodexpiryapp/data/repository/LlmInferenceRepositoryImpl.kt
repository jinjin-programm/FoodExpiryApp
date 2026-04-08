package com.example.foodexpiryapp.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.foodexpiryapp.data.remote.ModelDownloadManager
import com.example.foodexpiryapp.domain.model.DownloadUiState
import com.example.foodexpiryapp.domain.model.FoodIdentification
import com.example.foodexpiryapp.domain.model.ModelState
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import com.example.foodexpiryapp.inference.mnn.MnnLlmEngine
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

/**
 * Data layer implementation of [LlmInferenceRepository].
 *
 * Per D-01: Wraps MNN engine behind domain interface.
 * Per D-02: Single method [inferFood] handles everything internally.
 *
 * This class orchestrates:
 * 1. Model download check ([ModelDownloadManager])
 * 2. Model loading ([MnnLlmEngine])
 * 3. Inference ([MnnLlmEngine.runInference])
 * 4. JSON parsing (handled inside engine via [com.example.foodexpiryapp.inference.mnn.StructuredOutputParser])
 *
 * Per T-05-15: Mutex prevents concurrent inference calls (DoS mitigation).
 */
@Singleton
class LlmInferenceRepositoryImpl @Inject constructor(
    private val engine: MnnLlmEngine,
    private val downloadManager: ModelDownloadManager
) : LlmInferenceRepository {

    companion object {
        private const val TAG = "LlmInferenceRepo"
    }

    private val inferenceMutex = Mutex()

    // Supervised scope for observing download state — child failures won't cancel siblings
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotDownloaded)

    /**
     * Observable model state for UI display.
     * Maps [DownloadUiState] from [ModelDownloadManager] to domain [ModelState].
     * Per D-02: Status flows reactively through this StateFlow.
     */
    override val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    init {
        // Observe download state and map to domain ModelState
        scope.launch {
            downloadManager.observeDownloadState().collect { dlState ->
                _modelState.value = when (dlState) {
                    is DownloadUiState.Ready -> ModelState.Ready
                    is DownloadUiState.NotDownloaded -> ModelState.NotDownloaded
                    is DownloadUiState.WifiCheckRequired -> ModelState.WifiCheckRequired(dlState.estimatedSizeMB)
                    is DownloadUiState.Downloading -> ModelState.Downloading(dlState.overallProgress, dlState.currentFile)
                    is DownloadUiState.Verifying -> ModelState.Verifying(dlState.currentFile)
                    is DownloadUiState.Complete -> ModelState.Ready
                    is DownloadUiState.Error -> ModelState.Error(dlState.message)
                    is DownloadUiState.Paused -> ModelState.NotDownloaded
                }
            }
        }
    }

    /**
     * Identifies food from a bitmap image using on-device LLM.
     *
     * Internally handles:
     * 1. Download readiness check via [ModelDownloadManager]
     * 2. Model loading via [MnnLlmEngine] (lazy — only if not loaded)
     * 3. Inference via [MnnLlmEngine.runInference]
     * 4. Error handling with Chinese error messages for user-facing display
     *
     * @param bitmap The cropped food image to identify
     * @return Flow emitting a single [FoodIdentification] result
     */
    override fun inferFood(bitmap: Bitmap): Flow<FoodIdentification> = flow {
        inferenceMutex.withLock {
            try {
                // Step 1: Check if model is downloaded and ready
                if (!downloadManager.isModelReady()) {
                    Log.w(TAG, "Model not downloaded — cannot infer")
                    emit(
                        FoodIdentification(
                            name = "Error",
                            nameZh = "模型未下載",
                            confidence = 0f,
                            rawResponse = "Model not downloaded. Please download the AI model first."
                        )
                    )
                    return@flow
                }

                // Step 2: Load model if not already loaded
                if (!engine.isModelLoaded()) {
                    Log.d(TAG, "Loading LLM model...")
                    _modelState.value = ModelState.Loading
                    val loaded = engine.loadModel()
                    if (!loaded) {
                        Log.e(TAG, "Failed to load LLM model")
                        emit(
                            FoodIdentification(
                                name = "Error",
                                nameZh = "模型載入失敗",
                                confidence = 0f,
                                rawResponse = "Failed to load AI model. Please check device memory."
                            )
                        )
                        return@flow
                    }
                    _modelState.value = ModelState.Ready
                }

                // Step 3: Run inference
                Log.d(TAG, "Running food inference on ${bitmap.width}x${bitmap.height} bitmap")
                val result = engine.runInference(bitmap)

                if (result != null) {
                    Log.d(TAG, "Inference result: ${result.name} / ${result.nameZh} (confidence: ${result.confidence})")
                    emit(result)
                } else {
                    Log.w(TAG, "Inference returned null")
                    emit(
                        FoodIdentification(
                            name = "Unknown",
                            nameZh = "無法識別",
                            confidence = 0f,
                            rawResponse = "Could not identify food in image."
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Inference error", e)
                emit(
                    FoodIdentification(
                        name = "Error",
                        nameZh = "發生錯誤",
                        confidence = 0f,
                        rawResponse = "Error: ${e.message}"
                    )
                )
            }
        }
    }
}
