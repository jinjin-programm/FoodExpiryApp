package com.example.foodexpiryapp.presentation.ui.llm

import android.content.Context
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
        
        // Load the native library
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
    
    fun freeModel() {
        try {
            nativeFreeModel()
            isModelLoaded = false
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
    
    fun isLoaded(): Boolean = isModelLoaded
    
    // Native methods - implemented in C++
    private external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): Int
    private external fun nativeFreeModel()
    private external fun nativeGenerate(prompt: String): String
}
