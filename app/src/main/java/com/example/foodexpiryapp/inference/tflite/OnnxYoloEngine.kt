package com.example.foodexpiryapp.inference.tflite

import android.content.Context
import android.graphics.Bitmap
import com.example.foodexpiryapp.util.AppLog
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.inference.mnn.ModelLifecycleManager
import com.example.foodexpiryapp.inference.yolo.MnnYoloPostprocessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtException
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.min
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnnxYoloEngine @Inject constructor(
    private val lifecycleManager: ModelLifecycleManager,
    @ApplicationContext private val context: Context
) : YoloDetector {

    companion object {
        private const val TAG = "OnnxYoloEngine"
        private const val MODEL_PATH = "yoloe-26n-seg-pf.onnx"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val MAX_DETECTIONS = 8
        private const val IOU_THRESHOLD = 0.45f
    }

    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var isLoaded = false

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true

        if (!lifecycleManager.acquire(ModelLifecycleManager.ModelType.YOLO)) {
            AppLog.e(TAG, "Cannot acquire YOLO lifecycle")
            return@withContext false
        }

        try {
            val modelFile = copyAssetToTempFile(MODEL_PATH)
            val env = OrtEnvironment.getEnvironment()
            environment = env
            val options = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                addConfigEntry("session.intra_op.num_threads", "4")
            }
            session = environment?.createSession(modelFile.absolutePath, options)

            session?.inputNames?.let { inputs ->
                session?.outputNames?.let { outputs ->
                    AppLog.d(TAG, "Session created with inputs=$inputs, outputs=$outputs")
                }
            }

            isLoaded = true
            AppLog.d(TAG, "ONNX YOLOE model loaded successfully")
            true
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to load ONNX YOLOE model", e)
            lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
            false
        }
    }

    override fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (!isLoaded || session == null || environment == null) {
            AppLog.w(TAG, "detect() called but model not loaded")
            return emptyList()
        }

        return try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            AppLog.d(TAG, "Input bitmap: ${originalWidth}x${originalHeight}")

            val letterboxResult = createLetterboxBitmap(bitmap, INPUT_SIZE)
            val resized = letterboxResult.bitmap
            val inputTensor = convertBitmapToNCHWTensor(resized)

            AppLog.d(TAG, "Letterbox: scale=${letterboxResult.scale}, pad=(${letterboxResult.padW},${letterboxResult.padH}), resized=${resized.width}x${resized.height}")

            val inputName = session?.inputNames?.iterator()?.next() ?: "images"
            val results = session?.run(mapOf(inputName to inputTensor))

            val outputTensor = results?.get(0) as? OnnxTensor
            if (outputTensor == null) {
                AppLog.e(TAG, "Failed to get output tensor")
                return emptyList()
            }

            val outputBuffer = outputTensor.floatBuffer
            val shape = outputTensor.info.shape
            val numDetections = shape[1].toInt()
            val numValues = shape[2].toInt()

            val maskTensor = results?.get(1) as? OnnxTensor
            val numMasks = maskTensor?.info?.shape?.getOrNull(1)?.toInt() ?: 0
            AppLog.d(TAG, "Output shape: ${shape.toList()}, detections=$numDetections, values=$numValues, numMasks=$numMasks")

            val outputArray = FloatArray(outputBuffer.remaining())
            outputBuffer.get(outputArray)

            for (i in 0 until min(3, numDetections)) {
                val base = i * numValues
                if (base + numValues <= outputArray.size) {
                    val first15 = outputArray.sliceArray(base until base + min(15, numValues))
                    val last5 = outputArray.sliceArray(base + numValues - min(5, numValues) until base + numValues)
                    AppLog.d(TAG, "Raw det[$i] first15: ${first15.map { String.format("%.4f", it) }}")
                    AppLog.d(TAG, "Raw det[$i] last5: ${last5.map { String.format("%.4f", it) }}")
                }
            }

            val detections = MnnYoloPostprocessor.parseYoloEDetections(
                outputArray,
                numDetections,
                numValues,
                originalWidth,
                originalHeight,
                CONFIDENCE_THRESHOLD,
                MAX_DETECTIONS,
                letterboxResult.scale,
                letterboxResult.padW,
                letterboxResult.padH,
                numMasks
            )

            val nmsResults = MnnYoloPostprocessor.applyNms(detections, IOU_THRESHOLD)

            for ((index, det) in nmsResults.withIndex()) {
                AppLog.d(TAG, "Detection $index: label=${det.label}, conf=${String.format("%.2f", det.confidence)}, bbox=${det.boundingBox}")
            }

            nmsResults
        } catch (e: OrtException) {
            AppLog.e(TAG, "ONNX inference error", e)
            emptyList()
        } catch (e: Exception) {
            AppLog.e(TAG, "YOLOE detection error", e)
            emptyList()
        }
    }

    override suspend fun unloadModel() = withContext(Dispatchers.IO) {
        try {
            session?.close()
            AppLog.d(TAG, "ONNX YOLOE model unloaded")
        } catch (e: Exception) {
            AppLog.e(TAG, "Error unloading model", e)
        }
        session = null
        environment = null
        isLoaded = false
        lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
    }

    override fun isModelLoaded(): Boolean = isLoaded

    private data class LetterboxResult(
        val bitmap: Bitmap,
        val scale: Float,
        val padW: Float,
        val padH: Float
    )

    private fun createLetterboxBitmap(source: Bitmap, targetSize: Int): LetterboxResult {
        val srcW = source.width
        val srcH = source.height
        val scale = targetSize.toFloat() / maxOf(srcW, srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()

        val resized = Bitmap.createScaledBitmap(source, newW, newH, true)

        val padW = (targetSize - newW) / 2f
        val padH = (targetSize - newH) / 2f

        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.BLACK)
        canvas.drawBitmap(resized, padW, padH, null)

        if (resized != source) {
            resized.recycle()
        }

        return LetterboxResult(result, scale, padW, padH)
    }

    private fun copyAssetToTempFile(assetPath: String): File {
        val tempFile = File(context.cacheDir, assetPath)
        if (!tempFile.exists() || tempFile.length() == 0L) {
            context.assets.open(assetPath).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return tempFile
    }

    private fun convertBitmapToNCHWTensor(bitmap: Bitmap): OnnxTensor {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatBuffer = FloatBuffer.allocate(3 * width * height)

        for (c in 0 until 3) {
            for (h in 0 until height) {
                for (w in 0 until width) {
                    val pixel = pixels[h * width + w]
                    val value = when (c) {
                        0 -> ((pixel shr 16) and 0xFF) / 255.0f
                        1 -> ((pixel shr 8) and 0xFF) / 255.0f
                        else -> (pixel and 0xFF) / 255.0f
                    }
                    floatBuffer.put(value)
                }
            }
        }

        floatBuffer.rewind()
        return OnnxTensor.createTensor(
            environment,
            floatBuffer,
            longArrayOf(1, 3, height.toLong(), width.toLong())
        )
    }
}
