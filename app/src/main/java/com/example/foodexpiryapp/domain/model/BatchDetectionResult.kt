package com.example.foodexpiryapp.domain.model

import android.graphics.Bitmap

/**
 * Result of a complete batch detection session.
 *
 * Per YOLO-07: Processing caps at 8 items per scan.
 * Per YOLO-05: Each item classified sequentially by LLM.
 *
 * Contains all detection results ordered by confidence descending,
 * with counts for classified vs failed items.
 */
data class BatchDetectionResult(
    val sessionId: String,                        // UUID for this scan session
    val originalBitmap: Bitmap? = null,           // Full frame, null after processing to save memory
    val results: List<DetectionResult>,           // Ordered by confidence descending
    val totalCount: Int = results.size,           // Total detections
    val classifiedCount: Int = results.count { it.status == DetectionStatus.CLASSIFIED },
    val failedCount: Int = results.count { it.status == DetectionStatus.FAILED },
    val timestamp: Long = System.currentTimeMillis()
)
