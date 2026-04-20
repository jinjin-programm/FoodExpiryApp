package com.example.foodexpiryapp.inference.mnn

import android.content.Context
import android.graphics.Bitmap
import com.example.foodexpiryapp.util.AppLog
import com.example.foodexpiryapp.data.local.ModelStorageManager
import com.example.foodexpiryapp.domain.model.FoodIdentification
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnnLlmEngine @Inject constructor(
    private val config: MnnLlmConfig,
    private val lifecycleManager: ModelLifecycleManager,
    private val storageManager: ModelStorageManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MnnLlmEngine"
        private const val TEMP_IMAGE_NAME = "temp_food.jpg"

        init {
            try {
                MnnLlmNative
            } catch (_: UnsatisfiedLinkError) {
                AppLog.w(TAG, "MNN native libraries not available — LLM features unavailable")
            }
        }
    }

    private var nativeHandle: Long = 0L
    private var isLoaded = false
    private val debugImageSaver by lazy {
        DebugImageSaver(File(context.getExternalFilesDir(null), "mnn_debug_images"))
    }

    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true

        if (!MnnLlmNative.nativeLoaded) {
            AppLog.e(TAG, "Native libraries not loaded — cannot load model")
            return@withContext false
        }

        if (!storageManager.areAllModelFilesReady()) {
            AppLog.e(TAG, "Model files not ready — cannot load")
            return@withContext false
        }

        if (!lifecycleManager.acquire(ModelLifecycleManager.ModelType.LLM)) {
            AppLog.e(TAG, "Cannot acquire LLM lifecycle — another model active or insufficient memory")
            return@withContext false
        }

        try {
            val modelDir = storageManager.getModelDirectory().absolutePath
            AppLog.d(TAG, "Loading LLM model from: $modelDir")

            nativeHandle = MnnLlmNative.nativeCreateLlm(
                modelDir, config.threadNum,
                config.temperature, config.topP, config.topK, config.repetitionPenalty, config.maxNewTokens
            )

            if (nativeHandle == 0L) {
                AppLog.e(TAG, "Failed to create LLM native instance")
                lifecycleManager.release(ModelLifecycleManager.ModelType.LLM)
                return@withContext false
            }

            isLoaded = true
            AppLog.d(TAG, "LLM model loaded successfully (handle=$nativeHandle)")
            true
        } catch (e: Exception) {
            AppLog.e(TAG, "Error loading LLM model", e)
            lifecycleManager.release(ModelLifecycleManager.ModelType.LLM)
            nativeHandle = 0L
            false
        }
    }

    suspend fun runInference(bitmap: Bitmap, retryHint: String? = null): FoodIdentification? = withContext(Dispatchers.IO) {
        if (!isLoaded || nativeHandle == 0L) {
            AppLog.e(TAG, "Model not loaded — call loadModel() first")
            return@withContext null
        }

        try {
            // Resize the image to prevent out-of-memory and reduce processing time
            val targetSize = 420
            val width = bitmap.width
            val height = bitmap.height
            val resizedBitmap = if (width != targetSize || height != targetSize) {
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()
            val fingerprint = MessageDigest.getInstance("SHA-256")
                .digest(byteArray)
                .joinToString("") { "%02x".format(it) }
                .take(16)

            AppLog.d(TAG, "Prepared in-memory image: ${byteArray.size} bytes, ${resizedBitmap.width}x${resizedBitmap.height}, sha256=$fingerprint")
            val savedFile = debugImageSaver.saveJpeg(byteArray, "vision_input_$fingerprint")
            AppLog.d(TAG, "Saved debug input image to ${savedFile.absolutePath}")

            val rawResponse = if (retryHint != null) {
                MnnLlmNative.nativeRunInferenceWithHint(nativeHandle, byteArray, retryHint)
            } else {
                MnnLlmNative.nativeRunInference(nativeHandle, byteArray)
            }

            AppLog.d(TAG, "Raw LLM response: $rawResponse")

            val result = StructuredOutputParser.parse(rawResponse)

            result?.copy(rawResponse = rawResponse)
        } catch (e: Exception) {
            AppLog.e(TAG, "Inference failed", e)
            null
        }
    }

    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        if (nativeHandle != 0L) {
            try {
                MnnLlmNative.nativeDestroyLlm(nativeHandle)
                AppLog.d(TAG, "LLM model unloaded")
            } catch (e: Exception) {
                AppLog.e(TAG, "Error unloading model", e)
            }
            nativeHandle = 0L
            isLoaded = false
        }
        lifecycleManager.release(ModelLifecycleManager.ModelType.LLM)
    }

    fun isModelLoaded(): Boolean = isLoaded
}
