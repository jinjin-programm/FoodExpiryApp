package com.example.foodexpiryapp.domain.model

/**
 * Sealed class representing the state of the detection pipeline.
 *
 * Per YOLO-01: Pipeline emits progress state for UI observation.
 * Stages: Detecting (YOLO running) → Classifying (LLM on each crop) → Complete.
 *
 * Used as Flow emissions so UI can reactively update progress indicators.
 */
sealed class PipelineState {
    /** Pipeline is idle, no detection in progress. */
    data object Idle : PipelineState()

    /** Stage 1: YOLO model is running detection on the camera frame. */
    data object Detecting : PipelineState()

    /**
     * YOLO detection complete, raw detections available for overlay rendering.
     * Per D-06: Emitted before LLM classification starts so overlay can show bounding boxes.
     */
    data class Detected(val detections: List<DetectionResult>) : PipelineState()

    /** Stage 2: LLM is classifying crop [current] of [total]. */
    data class Classifying(val current: Int, val total: Int) : PipelineState()

    /** Stage 3: All detections classified, batch result ready. */
    data class Complete(val result: BatchDetectionResult) : PipelineState()

    /** Pipeline encountered an error. */
    data class Error(val message: String) : PipelineState()

    /** User cancelled the detection pipeline. */
    data object Cancelled : PipelineState()
}
