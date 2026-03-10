package com.example.foodexpiryapp.presentation.ui.llm

import android.graphics.Bitmap
import android.util.Log
import java.io.File

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
                Log.i(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }
    
    @Volatile
    private var isModelLoaded = false
    
    @Volatile
    private var hasVision = false
    
    fun loadModel(modelPath: String, contextSize: Int = 2048, threads: Int = 4): Boolean {
        return try {
            Log.i(TAG, "Loading model: $modelPath")
            Log.i(TAG, "Config: contextSize=$contextSize, threads=$threads")
            
            val file = File(modelPath)
            if (!file.exists()) {
                Log.e(TAG, "Model file not found: $modelPath")
                return false
            }
            
            val optimalThreads = if (threads <= 0) {
                val availableProcessors = Runtime.getRuntime().availableProcessors()
                availableProcessors.coerceIn(2, 4)
            } else {
                threads.coerceIn(1, 8)
            }
            
            Log.i(TAG, "Using $optimalThreads threads for inference")
            
            val result = nativeLoadModel(modelPath, contextSize, optimalThreads)
            isModelLoaded = result == 0
            
            if (isModelLoaded) {
                Log.i(TAG, "Model loaded successfully")
            } else {
                Log.e(TAG, "Failed to load model, error code: $result")
            }
            
            isModelLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading model: ${e.message}", e)
            false
        }
    }
    
    fun freeModel() {
        try {
            nativeFreeModel()
            isModelLoaded = false
            Log.i(TAG, "Model freed")
        } catch (e: Exception) {
            Log.e(TAG, "Exception freeing model: ${e.message}")
        }
    }
    
    fun isLoaded(): Boolean = isModelLoaded
    
    fun generate(prompt: String, maxTokens: Int = 256): String {
        if (!isModelLoaded) {
            return "Error: Model not loaded"
        }
        
        return try {
            val startTime = System.currentTimeMillis()
            val response = nativeGenerate(prompt, maxTokens)
            val elapsed = System.currentTimeMillis() - startTime
            
            Log.i(TAG, "Generated response in ${elapsed}ms, ${response.length} chars")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Generation error: ${e.message}", e)
            "Error: ${e.message}"
        }
    }
    
    fun loadMmproj(mmprojPath: String): Boolean {
        return try {
            Log.i(TAG, "Loading mmproj: $mmprojPath")
            
            val file = File(mmprojPath)
            if (!file.exists()) {
                Log.e(TAG, "Mmproj file not found: $mmprojPath")
                return false
            }
            
            val result = nativeLoadMmproj(mmprojPath)
            hasVision = result == 0
            
            if (hasVision) {
                Log.i(TAG, "Mmproj loaded successfully, vision enabled")
            } else {
                Log.e(TAG, "Failed to load mmproj, error code: $result")
            }
            
            hasVision
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading mmproj: ${e.message}", e)
            false
        }
    }
    
    fun hasVisionSupport(): Boolean = hasVision

    fun generateWithImage(prompt: String, bitmap: Bitmap, maxTokens: Int = 256): String {
        if (!isModelLoaded || !hasVision) {
            return if (!isModelLoaded) "Error: Model not loaded" else "Error: Vision not loaded"
        }

        return try {
            val startTime = System.currentTimeMillis()
            
            // Resize bitmap to a standard size for vision processing (e.g., 336x336 for Qwen2-VL/Llava)
            // Most vision encoders expect specific sizes, but the JNI can handle the scaling if we provide raw data.
            // Let's use the bitmap as is, but JNI will need width/height.
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val rgbData = ByteArray(width * height * 3)
            for (i in pixels.indices) {
                val p = pixels[i]
                rgbData[i * 3] = ((p shr 16) and 0xff).toByte()     // R
                rgbData[i * 3 + 1] = ((p shr 8) and 0xff).toByte()  // G
                rgbData[i * 3 + 2] = (p and 0xff).toByte()         // B
            }
            
            val response = nativeGenerateWithImage(prompt, rgbData, width, height, maxTokens)
            val elapsed = System.currentTimeMillis() - startTime
            
            Log.i(TAG, "Generated vision response in ${elapsed}ms")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Vision generation error: ${e.message}", e)
            "Error: ${e.message}"
        }
    }
    
    private external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): Int
    private external fun nativeFreeModel()
    private external fun nativeIsLoaded(): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeLoadMmproj(mmprojPath: String): Int
    private external fun nativeGenerateWithImage(prompt: String, rgbData: ByteArray, width: Int, height: Int, maxTokens: Int): String
}