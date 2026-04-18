package com.example.foodexpiryapp.inference.pipeline

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.foodexpiryapp.data.remote.ProviderConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioVisionClient
import com.example.foodexpiryapp.data.remote.ollama.OllamaVisionClient
import com.example.foodexpiryapp.domain.client.FoodVisionClient
import com.example.foodexpiryapp.domain.model.BatchDetectionResult
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.DetectionStatus
import com.example.foodexpiryapp.domain.model.PipelineState
import com.example.foodexpiryapp.inference.tflite.YoloDetector
import com.example.foodexpiryapp.inference.yolo.MnnYoloPostprocessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectionPipeline @Inject constructor(
    private val yoloDetector: YoloDetector,
    private val ollamaClient: OllamaVisionClient,
    private val lmStudioClient: LmStudioVisionClient,
    private val providerConfig: ProviderConfig,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DetectionPipeline"
    }

    private suspend fun getActiveClient(): FoodVisionClient {
        val provider = providerConfig.getProvider()
        return if (provider == ProviderConfig.PROVIDER_LMSTUDIO) lmStudioClient else ollamaClient
    }

    private fun saveCropToFile(bitmap: Bitmap, sessionId: String, index: Int): String? {
        return try {
            val cropDir = File(context.filesDir, "detection_crops")
            if (!cropDir.exists()) cropDir.mkdirs()
            val file = File(cropDir, "${sessionId}_${index}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save crop image for item $index", e)
            null
        }
    }

    fun detectAndClassify(bitmap: Bitmap): Flow<PipelineState> = channelFlow {
        val sessionId = UUID.randomUUID().toString()

        try {
            send(PipelineState.Detecting)

            if (!yoloDetector.isModelLoaded()) {
                val loaded = yoloDetector.loadModel()
                if (!loaded) {
                    send(PipelineState.Error("YOLO model load failed"))
                    return@channelFlow
                }
            }

            val detections = withContext(Dispatchers.Default) {
                yoloDetector.detect(bitmap)
            }

            yoloDetector.unloadModel()

            if (detections.isEmpty()) {
                send(PipelineState.Complete(
                    BatchDetectionResult(sessionId = sessionId, results = emptyList())
                ))
                return@channelFlow
            }

            val croppedDetections = detections.mapIndexed { index, det ->
                val crop = MnnYoloPostprocessor.cropDetection(bitmap, det)
                val cropPath = if (crop != null) saveCropToFile(crop, sessionId, index) else null
                det.copy(id = index, cropBitmap = crop, cropImagePath = cropPath)
            }

            send(PipelineState.Detected(croppedDetections))

            val client = getActiveClient()
            val results = mutableListOf<DetectionResult>()

            for ((index, detection) in croppedDetections.withIndex()) {
                if (!isActive) {
                    send(PipelineState.Cancelled)
                    return@channelFlow
                }

                send(PipelineState.Classifying(index + 1, croppedDetections.size))

                val result = if (detection.cropBitmap != null) {
                    try {
                        val identification = client.analyzeFood(detection.cropBitmap)
                        if (identification != null) {
                            detection.copy(
                                foodIdentification = identification,
                                status = DetectionStatus.CLASSIFIED
                            )
                        } else {
                            detection.copy(status = DetectionStatus.FAILED)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Classification failed for item ${index + 1}", e)
                        detection.copy(status = DetectionStatus.FAILED)
                    }
                } else {
                    detection.copy(status = DetectionStatus.FAILED)
                }

                detection.cropBitmap?.recycle()

                results.add(result.copy(cropBitmap = null))
            }

            val batchResult = BatchDetectionResult(
                sessionId = sessionId,
                results = results.sortedByDescending { it.confidence },
                totalCount = results.size,
                classifiedCount = results.count { it.status == DetectionStatus.CLASSIFIED },
                failedCount = results.count { it.status == DetectionStatus.FAILED },
                timestamp = System.currentTimeMillis()
            )
            send(PipelineState.Complete(batchResult))

        } catch (e: CancellationException) {
            send(PipelineState.Cancelled)
        } catch (e: Exception) {
            Log.e(TAG, "Pipeline error", e)
            send(PipelineState.Error(e.message ?: "Unknown error"))
        } finally {
            try { yoloDetector.unloadModel() } catch (_: Exception) {}
        }
    }
}
