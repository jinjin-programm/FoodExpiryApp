package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting detection results across process death.
 * Per D-17: Detection results stored in temporary Room table to survive process death during LLM inference.
 * Per D-20: ConfirmationFragment can read persisted results after process recreation.
 *
 * Each row represents one detected food item from a YOLO+LLM batch scan.
 */
@Entity(tableName = "detection_results")
data class DetectionResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** UUID grouping results from the same scan session */
    val sessionId: String,

    /** Order within the batch (for UI display ordering) */
    val indexInSession: Int,

    /** Food name from LLM classification (or "Unknown" if failed) */
    val foodName: String,

    /** Chinese name from LLM classification (or "" if failed) */
    val foodNameZh: String = "",

    /** FoodCategory enum name */
    val category: String = "OTHER",

    /** YOLO detection confidence (0-1) */
    val confidence: Float = 0.0f,

    /** LLM classification confidence (0-1) */
    val llmConfidence: Float = 0.0f,

    /** Status: "CLASSIFIED", "FAILED", or "PENDING" */
    val status: String = "PENDING",

    /** Bounding box coordinates (normalized 0-1) */
    val boundingBoxLeft: Float = 0.0f,
    val boundingBoxTop: Float = 0.0f,
    val boundingBoxRight: Float = 0.0f,
    val boundingBoxBottom: Float = 0.0f,

    /** Timestamp when this result was created */
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_CLASSIFIED = "CLASSIFIED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_PENDING = "PENDING"
    }
}