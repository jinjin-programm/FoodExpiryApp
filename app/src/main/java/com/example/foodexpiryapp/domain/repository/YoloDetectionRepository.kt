package com.example.foodexpiryapp.domain.repository

import android.graphics.Bitmap
import com.example.foodexpiryapp.domain.model.PipelineState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain interface for YOLO+LLM batch food detection.
 *
 * Per D-01: Domain interface — implementation in data layer.
 * Per YOLO-01: Detects multiple food items in a single camera frame.
 *
 * The pipeline follows a two-stage process:
 * 1. YOLO detects food items and produces bounding boxes
 * 2. Each detected region is cropped and classified by the LLM sequentially
 *
 * Progress is observable via [pipelineState] StateFlow and the Flow
 * returned by [detectFoods].
 */
interface YoloDetectionRepository {

    /**
     * Runs the full detection pipeline on a camera frame bitmap.
     *
     * Per YOLO-01: YOLO detects multiple food items in a single frame.
     * Per YOLO-05: Each detected region is cropped and classified by LLM.
     * Per YOLO-07: Processing caps at 8 items per scan.
     * Per YOLO-04: LLM classification runs sequentially for each crop.
     *
     * @param bitmap Camera frame to detect food items in
     * @return Flow emitting [PipelineState] transitions:
     *   Detecting → Classifying(n/total) → Complete
     */
    fun detectFoods(bitmap: Bitmap): Flow<PipelineState>

    /**
     * Cancels an in-progress detection pipeline.
     * Per YOLO-03: Pipeline supports cancellation between items.
     */
    suspend fun cancelDetection()

    /**
     * Observable pipeline state for UI to display status reactively.
     */
    val pipelineState: StateFlow<PipelineState>
}
