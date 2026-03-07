package com.example.foodexpiryapp.presentation.ui.llm

import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LlamaBridge private constructor() {
    
    companion object {
        private const val TAG = "LlamaBridge"
        
        @Volatile
        private var instance: LlamaBridge? = null
        
        fun getInstance(): LlamaBridge {
            return instance ?: synchronized(this) {
                instance ?: LlamaBridge().also { instance = it }
            }
        }
        
        init {
            try {
                System.loadLibrary("llama_jni")
                Log.i(TAG, "Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }
    
    @Volatile
    private var isModelLoaded = false
    @Volatile
    private var hasVision = false
    
    fun loadModel(modelPath: String, contextSize: Int = 4096, threads: Int = 4): Boolean {
        return try {
            Log.i(TAG, "Loading model from: $modelPath")
            val result = nativeLoadModel(modelPath, contextSize, threads)
            isModelLoaded = result == 0
            Log.i(TAG, "Model load result: $result")
            isModelLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            false
        }
    }
    
    fun loadMmproj(mmprojPath: String): Boolean {
        return try {
            Log.i(TAG, "Loading mmproj from: $mmprojPath")
            val result = nativeLoadMmproj(mmprojPath)
            if (result == 0) {
                hasVision = nativeHasVision()
                Log.i(TAG, "Mmproj loaded, vision support: $hasVision")
                true
            } else {
                Log.e(TAG, "Failed to load mmproj: $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading mmproj: ${e.message}", e)
            false
        }
    }
    
    fun freeModel() {
        try {
            nativeFreeModel()
            isModelLoaded = false
            hasVision = false
            Log.i(TAG, "Model freed")
        } catch (e: Exception) {
            Log.e(TAG, "Error freeing model: ${e.message}")
        }
    }
    
    fun generate(prompt: String): String {
        if (!isModelLoaded) {
            return "Error: Model not loaded"
        }
        return try {
            nativeGenerate(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating: ${e.message}", e)
            "Error: ${e.message}"
        }
    }
    
    fun generateWithImage(prompt: String, bitmap: Bitmap): String {
        if (!isModelLoaded) {
            return "Error: Model not loaded"
        }
        if (!hasVision) {
            return "Error: Vision model not loaded (mmproj required)"
        }
        return try {
            val rgbData = bitmapToRgbData(bitmap)
            nativeGenerateWithImage(prompt, rgbData, bitmap.width, bitmap.height)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating with image: ${e.message}", e)
            "Error: ${e.message}"
        }
    }
    
    fun isLoaded(): Boolean = isModelLoaded
    
    fun hasVisionSupport(): Boolean = hasVision
    
    private fun bitmapToRgbData(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val rgbData = ByteArray(width * height * 3)
        var index = 0
        for (pixel in pixels) {
            rgbData[index++] = ((pixel shr 16) and 0xFF).toByte() // R
            rgbData[index++] = ((pixel shr 8) and 0xFF).toByte()  // G
            rgbData[index++] = (pixel and 0xFF).toByte()          // B
        }
        return rgbData
    }
    
    private external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): Int
    private external fun nativeLoadMmproj(mmprojPath: String): Int
    private external fun nativeFreeModel()
    private external fun nativeGenerate(prompt: String): String
    private external fun nativeGenerateWithImage(prompt: String, imageData: ByteArray, width: Int, height: Int): String
    private external fun nativeHasVision(): Boolean
}