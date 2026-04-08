package com.example.foodexpiryapp.inference.yolo

/**
 * Configuration for the MNN-based YOLO food detection engine.
 *
 * Per D-01: YOLO model bundled in APK assets (~5-20MB).
 * Per D-02: YOLO runs through MNN engine (same AAR as LLM).
 * Per D-03: YOLO model format must be MNN-compatible.
 * Per YOLO-07: Max 5-8 items per scan.
 */
data class MnnYoloConfig(
    val modelAssetPath: String = "yolo_food.mnn",  // MNN-format YOLO model in assets
    val inputSize: Int = 640,                       // YOLO input resolution (640x640)
    val confidenceThreshold: Float = 0.25f,         // Minimum detection confidence
    val iouThreshold: Float = 0.45f,                // NMS IoU threshold
    val maxDetections: Int = 8                       // Max items per scan per YOLO-07
)
