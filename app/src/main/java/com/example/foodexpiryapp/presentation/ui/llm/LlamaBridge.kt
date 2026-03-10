package com.example.foodexpiryapp.presentation.ui.llm

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
    
    private external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): Int
    private external fun nativeFreeModel()
    private external fun nativeIsLoaded(): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeLoadMmproj(mmprojPath: String): Int
}