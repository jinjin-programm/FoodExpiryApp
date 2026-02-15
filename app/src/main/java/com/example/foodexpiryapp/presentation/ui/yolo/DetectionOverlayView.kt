package com.example.foodexpiryapp.presentation.ui.yolo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave

/**
 * Custom view for drawing detection bounding boxes on camera preview
 */
class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val detections = mutableListOf<DetectionResult>()
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.GREEN
    }
    
    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textSize = 40f
        color = Color.WHITE
        isFakeBoldText = true
    }
    
    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#80000000")
    }

    fun setDetections(detections: List<DetectionResult>, imageWidth: Int, imageHeight: Int) {
        this.detections.clear()
        this.detections.addAll(detections)
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
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
            val scaledBox = RectF(
                detection.boundingBox.left * scaleX,
                detection.boundingBox.top * scaleY,
                detection.boundingBox.right * scaleX,
                detection.boundingBox.bottom * scaleY
            )
            
            // Draw bounding box with color based on confidence
            boxPaint.color = when {
                detection.confidence > 0.8f -> Color.GREEN
                detection.confidence > 0.6f -> Color.YELLOW
                else -> Color.RED
            }
            
            canvas.drawRect(scaledBox, boxPaint)
            
            // Draw label background
            val label = "${detection.label} ${(detection.confidence * 100).toInt()}%"
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(label, 0, label.length, textBounds)
            
            val backgroundRect = RectF(
                scaledBox.left,
                scaledBox.top - textBounds.height() - 16f,
                scaledBox.left + textBounds.width() + 16f,
                scaledBox.top
            )
            
            canvas.drawRect(backgroundRect, textBackgroundPaint)
            
            // Draw label text
            canvas.drawText(
                label,
                scaledBox.left + 8f,
                scaledBox.top - 8f,
                textPaint
            )
        }
    }
}
