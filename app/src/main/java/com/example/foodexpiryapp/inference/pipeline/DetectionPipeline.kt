package com.example.foodexpiryapp.inference.pipeline

import android.graphics.Bitmap
import android.util.Log
import com.example.foodexpiryapp.domain.model.BatchDetectionResult
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.DetectionStatus
import com.example.foodexpiryapp.domain.model.PipelineState
import com.example.foodexpiryapp.inference.mnn.MnnLlmEngine
import com.example.foodexpiryapp.inference.mnn.ModelLifecycleManager
import com.example.foodexpiryapp.inference.yolo.MnnYoloEngine
import com.example.foodexpiryapp.inference.yolo.MnnYoloPostprocessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core orchestrator for the YOLO+LLM batch detection pipeline.
 *
 * Per D-05: Unified detection pipeline — YOLO always runs first, then LLM classifies each crop.
 * Per D-08/YOLO-04: Sequential LLM classification (one crop at a time).
 * Per YOLO-08: Bitmaps recycled immediately after classification.
 * Per PITFALL-1: Sequential model lifecycle — never both YOLO and LLM loaded simultaneously.
 * Per PITFALL-6: Prevent memory cascade via immediate bitmap recycling.
 *
 * Pipeline stages:
 * 1. YOLO Detection — detect food items in camera frame
 * 2. Crop Regions — extract bounding box regions from original bitmap
 * 3. Sequential LLM Classification — classify each crop one at a time
 * 4. Complete — emit BatchDetectionResult with all results
 */
@Singleton
class DetectionPipeline @Inject constructor(
    private val yoloEngine: MnnYoloEngine,
    private val llmEngine: MnnLlmEngine,
    private val lifecycleManager: ModelLifecycleManager
) {
    companion object {
        private const val TAG = "DetectionPipeline"
    }

    /**
     * Runs the full detection pipeline on a bitmap image.
     *
     * Emits [PipelineState] transitions:
     *   Detecting → Classifying(n/total) → Complete
     *
     * Lifecycle:
     *   1. Acquire YOLO → detect → release YOLO
     *   2. Acquire LLM → classify each crop → release LLM
     *
     * Per PITFALL-1: Models are NEVER loaded simultaneously.
     * Per YOLO-03: Cancellation supported between items via isActive check.
     */
    fun detectAndClassify(bitmap: Bitmap): Flow<PipelineState> = channelFlow {
        val sessionId = UUID.randomUUID().toString()

        try {
            // Stage 1: YOLO Detection
            send(PipelineState.Detecting)

            if (!yoloEngine.isModelLoaded()) {
                val loaded = yoloEngine.loadModel()
                if (!loaded) {
                    send(PipelineState.Error("YOLO model load failed"))
                    return@channelFlow
                }
            }

            val detections = yoloEngine.detect(bitmap)

            // Release YOLO before loading LLM (mutual exclusion per PITFALL-1)
            yoloEngine.unloadModel()

            if (detections.isEmpty()) {
                send(PipelineState.Complete(
                    BatchDetectionResult(sessionId = sessionId, results = emptyList())
                ))
                return@channelFlow
            }

            // Crop regions from original bitmap
            val croppedDetections = detections.mapIndexed { index, det ->
                val crop = MnnYoloPostprocessor.cropDetection(bitmap, det)
                det.copy(id = index, cropBitmap = crop)
            }

            // Stage 2: Sequential LLM classification
            val results = mutableListOf<DetectionResult>()

            if (!llmEngine.isModelLoaded()) {
                val loaded = llmEngine.loadModel()
                if (!loaded) {
                    send(PipelineState.Error("LLM model load failed"))
                    return@channelFlow
                }
            }

            for ((index, detection) in croppedDetections.withIndex()) {
                // Check cancellation per YOLO-03
                if (!isActive) {
                    send(PipelineState.Cancelled)
                    return@channelFlow
                }

                send(PipelineState.Classifying(index + 1, croppedDetections.size))

                val result = if (detection.cropBitmap != null) {
                    try {
                        val identification = llmEngine.runInference(detection.cropBitmap)
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

                // Recycle crop bitmap IMMEDIATELY per YOLO-08/PITFALL-6
                detection.cropBitmap?.recycle()

                results.add(result)
            }

            // Stage 3: Complete
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
            // Ensure LLM is unloaded after pipeline completes
            try { llmEngine.unloadModel() } catch (_: Exception) {}
        }
    }
}
