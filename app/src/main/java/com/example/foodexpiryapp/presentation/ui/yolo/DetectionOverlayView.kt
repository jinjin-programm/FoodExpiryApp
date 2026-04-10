package com.example.foodexpiryapp.presentation.ui.yolo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.DetectionStatus

/**
 * Custom view for drawing detection bounding boxes on camera preview.
 *
 * Per D-07/D-08/D-09: Two-phase visual state:
 *   PENDING  → Blue-gray border (detected by YOLO, awaiting LLM classification)
 *   CLASSIFIED → Green border + food name label (LLM identified the item)
 *   FAILED   → Red border (LLM classification failed)
 *
 * No dashed animation — color changes only per D-08.
 */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val detections = mutableListOf<DetectionResult>()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    // Per D-07: Status-based paints for bounding box rendering
    private val pendingPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = context.getColor(R.color.detection_box_detected)
    }

    private val classifiedPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = context.getColor(R.color.detection_box_classified)
    }

    private val failedPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = context.getColor(R.color.detection_box_failed)
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 40f
        color = android.graphics.Color.WHITE
        isFakeBoldText = true
    }

    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#80000000")
    }

    fun setDetections(detections: List<DetectionResult>, imageWidth: Int, imageHeight: Int) {
        this.detections.clear()
        this.detections.addAll(detections)
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    /**
     * Per D-09: Update a single detection's status (e.g., after LLM classification).
     * Invalidates the view to redraw with updated visual state.
     */
    fun updateDetection(updated: DetectionResult) {
        val index = detections.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            detections[index] = updated
            invalidate()
        }
    }

    fun clearDetections() {
        detections.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detections.isEmpty() || imageWidth == 0 || imageHeight == 0) return

        // Calculate scale factors to map image coordinates to view coordinates
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight

        detections.forEach { detection ->
            // Show bounding box when confidence is higher than 40%
            if (detection.confidence <= 0.4f) return@forEach

            val scaledBox = RectF(
                detection.boundingBox.left * scaleX,
                detection.boundingBox.top * scaleY,
                detection.boundingBox.right * scaleX,
                detection.boundingBox.bottom * scaleY
            )

            // Per D-07/D-09: Select paint based on detection status
            val paint = when (detection.status) {
                DetectionStatus.PENDING -> pendingPaint
                DetectionStatus.CLASSIFIED -> classifiedPaint
                DetectionStatus.FAILED -> failedPaint
            }

            // Draw bounding box with status-appropriate color
            canvas.drawRect(scaledBox, paint)

            // Per D-07: Show label only for CLASSIFIED items
            if (detection.status == DetectionStatus.CLASSIFIED) {
                val foodName = detection.foodIdentification?.name
                if (foodName != null) {
                    val label = "$foodName ${(detection.confidence * 100).toInt()}%"
                    drawLabel(canvas, scaledBox, label)
                }
            } else if (detection.status == DetectionStatus.PENDING) {
                // Show confidence percentage for PENDING items (no food name yet)
                val label = "${(detection.confidence * 100).toInt()}%"
                drawLabel(canvas, scaledBox, label)
            }
        }
    }

    private fun drawLabel(canvas: Canvas, scaledBox: RectF, label: String) {
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        val backgroundRect = RectF(
            scaledBox.left,
            scaledBox.top - textBounds.height() - 16f,
            scaledBox.left + textBounds.width() + 16f,
            scaledBox.top
        )

        canvas.drawRect(backgroundRect, textBackgroundPaint)
        canvas.drawText(
            label,
            scaledBox.left + 8f,
            scaledBox.top - 8f,
            textPaint
        )
    }
}
