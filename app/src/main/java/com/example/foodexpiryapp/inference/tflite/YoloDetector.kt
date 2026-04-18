package com.example.foodexpiryapp.inference.tflite

import android.graphics.Bitmap
import com.example.foodexpiryapp.domain.model.DetectionResult

interface YoloDetector {
    suspend fun loadModel(): Boolean
    fun detect(bitmap: Bitmap): List<DetectionResult>
    suspend fun unloadModel()
    fun isModelLoaded(): Boolean
}
