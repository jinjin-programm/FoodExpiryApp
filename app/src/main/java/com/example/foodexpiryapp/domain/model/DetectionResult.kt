package com.example.foodexpiryapp.domain.model

import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Status of an individual detection within a batch pipeline.
 */
enum class DetectionStatus {
    PENDING,    // Detected by YOLO, awaiting LLM classification
    CLASSIFIED, // LLM classification succeeded
    FAILED      // LLM classification failed
}

/**
 * Domain-level detection result for a single food item.
 *
 * This is the domain model used by the YOLO+LLM pipeline.
 * It is distinct from the presentation-layer DetectionResult
 * in [com.example.foodexpiryapp.presentation.ui.yolo.DetectionResult]
 * which operates in 640x640 coordinate space.
 *
 * Per YOLO-01: YOLO detects multiple food items in a single camera frame.
 * Per YOLO-05: Each detected region is cropped and classified by the local LLM.
 */
data class DetectionResult(
    val id: Int = 0,                              // Sequential index for batch tracking
    val boundingBox: RectF,                        // Normalized 0f-1f coordinates relative to original image
    val cropBitmap: Bitmap? = null,                // Cropped region, null if crop failed
    val label: String,                             // YOLO class label (e.g., "apple")
    val category: FoodCategory,                    // Mapped from YOLO label
    val confidence: Float,                         // YOLO detection confidence
    val foodIdentification: FoodIdentification? = null,  // LLM result, null until classified
    val status: DetectionStatus = DetectionStatus.PENDING  // Processing status
)
