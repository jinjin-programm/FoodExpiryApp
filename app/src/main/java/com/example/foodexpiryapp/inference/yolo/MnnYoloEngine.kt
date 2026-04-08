package com.example.foodexpiryapp.inference.yolo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.inference.mnn.ModelLifecycleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MNN-based YOLO food detection engine.
 *
 * Follows the same lifecycle pattern as [MnnLlmEngine]:
 * - loadModel() acquires ModelLifecycleManager.ModelType.YOLO
 * - detect() runs YOLO inference on a bitmap
 * - unloadModel() releases YOLO lifecycle
 *
 * Per D-02: YOLO runs through MNN engine (same AAR as LLM).
 * Per PITFALL-1: Mutual exclusion with LLM — never both loaded simultaneously.
 * Per PITFALL-5: All native calls on Dispatchers.IO.
 * Per YOLO-07: Results capped at maxDetections (default 8).
 *
 * NOTE: The actual MNN native calls for YOLO are JNI stubs initially.
 * The MNN AAR provides the actual inference at runtime. For now, the
 * detect() method preprocesses, calls a stub, and postprocesses.
 */
@Singleton
class MnnYoloEngine @Inject constructor(
    private val config: MnnYoloConfig,
    private val lifecycleManager: ModelLifecycleManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MnnYoloEngine"
    }

    private var isLoaded = false

    /**
     * Loads the YOLO model from APK assets.
     * Per PITFALL-5: Must be called on Dispatchers.IO.
     * Per PITFALL-1: Acquires YOLO lifecycle for mutual exclusion with LLM.
     *
     * @return true if model loaded successfully, false otherwise
     */
    suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true

        // Acquire lifecycle per MNN-05 / PITFALL-1
        if (!lifecycleManager.acquire(ModelLifecycleManager.ModelType.YOLO)) {
            Log.e(TAG, "Cannot acquire YOLO lifecycle — another model active or insufficient memory")
            return@withContext false
        }

        try {
            // Load MNN-format YOLO model from assets
            // Per D-01: Model bundled in APK assets (~5-20MB)
            val modelPath = config.modelAssetPath
            Log.d(TAG, "Loading YOLO model from assets: $modelPath")

            // Verify asset exists
            try {
                context.assets.open(modelPath).close()
                Log.d(TAG, "YOLO model asset found: $modelPath")
            } catch (e: Exception) {
                Log.e(TAG, "YOLO model asset not found: $modelPath", e)
                lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
                return@withContext false
            }

            // TODO: Initialize MNN YOLO session via JNI when native bridge is ready
            // For now, mark as loaded since the asset exists
            isLoaded = true
            Log.d(TAG, "YOLO model loaded successfully (stub mode)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading YOLO model", e)
            lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
            isLoaded = false
            false
        }
    }

    /**
     * Runs YOLO detection on a bitmap image.
     *
     * Per YOLO-01: Detects multiple food items in a single frame.
     * Per YOLO-07: Results capped at maxDetections (default 8).
     * Per PITFALL-5: All on Dispatchers.IO (caller's responsibility for suspend).
     *
     * Processing pipeline:
     * 1. Preprocess bitmap (letterbox resize to 640x640, normalize)
     * 2. Run MNN inference (stub returning empty array until native bridge ready)
     * 3. Postprocess with MnnYoloPostprocessor (NMS, coordinate normalization)
     * 4. Return capped list
     *
     * @param bitmap Input image to detect food items in
     * @return List of DetectionResult, capped at maxDetections
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (!isLoaded) {
            Log.w(TAG, "detect() called but model not loaded")
            return emptyList()
        }

        return try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            // Step 1: Preprocess — letterbox resize to 640x640
            val preprocessed = preprocessBitmap(bitmap)

            // Step 2: Run MNN inference (stub — returns empty until native bridge ready)
            // TODO: Replace with actual MNN native inference call
            // val outputArray = MnnYoloNative.nativeRunDetection(nativeHandle, preprocessed)
            val outputArray = floatArrayOf() // Stub: no detections until native bridge implemented
            val numDetections = 0
            val numValues = 6

            // Step 3: Postprocess results
            val detections = MnnYoloPostprocessor.parseDetections(
                outputArray, numDetections, numValues,
                originalWidth, originalHeight
            )

            // Step 4: Apply NMS and cap at maxDetections
            val nmsResults = MnnYoloPostprocessor.applyNms(detections, config.iouThreshold)

            nmsResults.take(config.maxDetections)
        } catch (e: Exception) {
            Log.e(TAG, "YOLO detection error", e)
            emptyList()
        }
    }

    /**
     * Unloads the YOLO model and releases native resources.
     * Per MNN-05: Must be called before loading another model.
     */
    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        try {
            // TODO: Free MNN YOLO session via JNI when native bridge is ready
            // MnnYoloNative.nativeDestroyYolo(nativeHandle)
            isLoaded = false
            Log.d(TAG, "YOLO model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading YOLO model", e)
        }
        lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
    }

    /**
     * Checks if the YOLO model is currently loaded.
     */
    fun isModelLoaded(): Boolean = isLoaded

    /**
     * Preprocesses a bitmap for YOLO inference.
     * Applies letterbox resize to 640x640 and normalization.
     *
     * Per D-03: YOLO model expects 640x640 normalized input.
     */
    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        val inputSize = config.inputSize

        // Letterbox resize: maintain aspect ratio, pad to square
        val width = bitmap.width
        val height = bitmap.height
        val scale = inputSize.toFloat() / maxOf(width, height)

        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
}
