package com.example.foodexpiryapp.inference.mnn

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.foodexpiryapp.data.local.ModelStorageManager
import com.example.foodexpiryapp.domain.model.FoodIdentification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MNN LLM inference engine for food identification.
 * Per MNN-06: Exposed as Hilt singleton with clean Kotlin API.
 * Per D-01: NOT exposed directly to ViewModels (use LlmInferenceRepository).
 * Per PITFALL-5: All native calls on Dispatchers.IO.
 *
 * This engine wraps the MNN LLM C++ module (libllm.so) via JNI.
 * The JNI bridge (MnnLlmNative.kt) provides the native method declarations.
 */
@Singleton
class MnnLlmEngine @Inject constructor(
    private val config: MnnLlmConfig,
    private val lifecycleManager: ModelLifecycleManager,
    private val storageManager: ModelStorageManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MnnLlmEngine"

        init {
            try {
                System.loadLibrary("mnn_llm_bridge")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "libmnn_llm_bridge.so not found — LLM features unavailable", e)
            }
        }

        // Qwen3.5 prompt template per PITFALL-13
        private const val SYSTEM_PROMPT = """You are a food identification assistant. Identify the food in the image and respond ONLY with a JSON object in this exact format:
{"name": "english name", "name_zh": "中文名", "category": "category", "confidence": 0.95}

Rules:
- "name" must be in English, lowercase
- "name_zh" must be in Traditional Chinese (繁體中文)
- "confidence" must be 0.0-1.0
- Respond with ONLY the JSON, no other text"""
    }

    private var nativeHandle: Long = 0L
    private var isLoaded = false

    /**
     * Loads the LLM model from downloaded files.
     * Per PITFALL-5: Must be called on Dispatchers.IO.
     * Per PITFALL-3: Only loads from finalized files (not .part).
     * Per MNN-03: Loads Qwen3.5-2B-MNN.
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true

        if (!storageManager.areAllModelFilesReady()) {
            Log.e(TAG, "Model files not ready — cannot load")
            return@withContext false
        }

        // Acquire lifecycle per MNN-05
        if (!lifecycleManager.acquire(ModelLifecycleManager.ModelType.LLM)) {
            Log.e(TAG, "Cannot acquire LLM lifecycle — another model active or insufficient memory")
            return@withContext false
        }

        try {
            val modelDir = storageManager.getModelDirectory().absolutePath
            Log.d(TAG, "Loading LLM model from: $modelDir")

            // Load via native JNI bridge
            // The native handle is a pointer to the C++ LLM session
            nativeHandle = MnnLlmNative.nativeCreateLlm(modelDir, config.threadNum, config.memoryMode)

            if (nativeHandle == 0L) {
                Log.e(TAG, "Failed to create LLM native instance")
                lifecycleManager.release(ModelLifecycleManager.ModelType.LLM)
                return@withContext false
            }

            isLoaded = true
            Log.d(TAG, "LLM model loaded successfully (handle=$nativeHandle)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading LLM model", e)
            lifecycleManager.release(ModelLifecycleManager.ModelType.LLM)
            nativeHandle = 0L
            false
        }
    }

    /**
     * Runs inference on a food image.
     * Per MNN-04: Returns structured JSON with name and name_zh.
     * Per PITFALL-5: All on Dispatchers.IO.
     *
     * @param bitmap Food image to identify (should be pre-cropped)
     * @return FoodIdentification result or null on failure
     */
    suspend fun runInference(bitmap: Bitmap): FoodIdentification? = withContext(Dispatchers.IO) {
        if (!isLoaded || nativeHandle == 0L) {
            Log.e(TAG, "Model not loaded — call loadModel() first")
            return@withContext null
        }

        try {
            // Convert bitmap to byte array for multimodal input (Qwen3.5 supports vision)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val imageData = stream.toByteArray()

            // Build prompt using Qwen3.5 template per PITFALL-13
            val prompt = buildPrompt()

            // Run inference via native bridge
            val rawResponse = MnnLlmNative.nativeRunInference(
                nativeHandle, imageData, bitmap.width, bitmap.height
            )

            Log.d(TAG, "Raw LLM response: $rawResponse")

            // Parse structured output
            val result = StructuredOutputParser.parse(rawResponse)

            result?.copy(rawResponse = rawResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        }
    }

    /**
     * Unloads the model and releases native resources.
     * Per MNN-05: Must be called before loading another model.
     */
    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        if (nativeHandle != 0L) {
            try {
                MnnLlmNative.nativeDestroyLlm(nativeHandle)
                Log.d(TAG, "LLM model unloaded")
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading model", e)
            }
            nativeHandle = 0L
            isLoaded = false
        }
        lifecycleManager.release(ModelLifecycleManager.ModelType.LLM)
    }

    /**
     * Checks if model is currently loaded.
     */
    fun isModelLoaded(): Boolean = isLoaded

    private fun buildPrompt(): String {
        // Qwen3.5 VL format: <|im_start|>system\n...<|im_end|>\n<|im_start|>user\n...<|im_end|>\n<|im_start|>assistant\n
        return "<|im_start|>system\n$SYSTEM_PROMPT<|im_end|>\n" +
               "<|im_start|>user\nWhat food is in this image?<|im_end|>\n" +
               "<|im_start|>assistant\n"
    }
}
