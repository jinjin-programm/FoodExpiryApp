package com.example.foodexpiryapp.inference.yolo

import android.content.Context
import android.graphics.Bitmap
import com.example.foodexpiryapp.util.AppLog
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.inference.mnn.ModelLifecycleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
    private var nativeHandle: Long = 0L

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
            AppLog.e(TAG, "Cannot acquire YOLO lifecycle — another model active or insufficient memory")
            return@withContext false
        }

        try {
            // Load MNN-format YOLO model from assets
            // Per D-01: Model bundled in APK assets (~5-20MB)
            val modelPath = config.modelAssetPath
            AppLog.d(TAG, "Loading YOLO model from assets: $modelPath")

            // Verify asset exists per T-08-01-03 (threat mitigation: check asset before native call)
            try {
                context.assets.open(modelPath).close()
                AppLog.d(TAG, "YOLO model asset found: $modelPath")
            } catch (e: Exception) {
                AppLog.e(TAG, "YOLO model asset not found: $modelPath", e)
                lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
                return@withContext false
            }

            // Initialize MNN YOLO session via JNI
            // Per D-04: Custom JNI bridge pattern (same as MnnLlmNative)
            nativeHandle = MnnYoloNative.nativeCreateYolo(modelPath, 4) // 4 threads default
            if (nativeHandle == 0L) {
                AppLog.e(TAG, "Failed to create YOLO native instance")
                lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
                return@withContext false
            }

            isLoaded = true
            AppLog.d(TAG, "YOLO model loaded successfully (handle=$nativeHandle)")
            true
        } catch (e: Exception) {
            AppLog.e(TAG, "Error loading YOLO model", e)
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
 * 1. Encode bitmap to JPEG byte array
 * 2. Run MNN YOLO inference via native bridge (mnn_yolo_bridge.cpp)
 * 3. Postprocess with MnnYoloPostprocessor (NMS, coordinate normalization)
 * 4. Return capped list
     *
     * @param bitmap Input image to detect food items in
     * @return List of DetectionResult, capped at maxDetections
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (!isLoaded) {
            AppLog.w(TAG, "detect() called but model not loaded")
            return emptyList()
        }

        return try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            // Step 1: Encode bitmap to JPEG byte array for JNI transfer
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteArray = stream.toByteArray()

            // Step 2: Run YOLO inference via native bridge
            val outputArray = MnnYoloNative.nativeRunDetection(nativeHandle, byteArray, originalWidth, originalHeight)

            // Step 3: Determine detection count and value count from output
            // Per D-05: Output format is [x1,y1,x2,y2,confidence,class_id,...] per row (6+ values)
            val numValues = 6  // Minimum values per detection row
            val numDetections = outputArray.size / numValues

            // Step 4: Postprocess results
            val detections = MnnYoloPostprocessor.parseDetections(
                outputArray, numDetections, numValues,
                originalWidth, originalHeight
            )

            // Step 5: Apply NMS and cap at maxDetections
            val nmsResults = MnnYoloPostprocessor.applyNms(detections, config.iouThreshold)

            nmsResults.take(config.maxDetections)
        } catch (e: Exception) {
            AppLog.e(TAG, "YOLO detection error", e)
            emptyList()
        }
    }

    /**
     * Unloads the YOLO model and releases native resources.
     * Per MNN-05: Must be called before loading another model.
     */
    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        try {
            if (nativeHandle != 0L) {
                MnnYoloNative.nativeDestroyYolo(nativeHandle)
                AppLog.d(TAG, "YOLO model unloaded (native handle released)")
            }
            nativeHandle = 0L
            isLoaded = false
        } catch (e: Exception) {
            AppLog.e(TAG, "Error unloading YOLO model", e)
            nativeHandle = 0L
            isLoaded = false
        }
        lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
    }

    /**
     * Checks if the YOLO model is currently loaded.
     */
    fun isModelLoaded(): Boolean = isLoaded
}
