package com.example.foodexpiryapp.inference.mnn

import android.util.Log

/**
 * JNI native method declarations for MNN LLM C++ bridge.
 * Methods are implemented in mnn_llm_bridge.cpp.
 * Reference: MNN apps/Android/MnnLlmChat
 */
object MnnLlmNative {
    private const val TAG = "MnnLlmNative"

    var nativeLoaded = false
        private set

    init {
        try {
            try {
                System.loadLibrary("c++_shared")
                Log.i(TAG, "c++_shared loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.i(TAG, "c++_shared already loaded or not available: ${e.message}")
            }
            System.loadLibrary("MNN"); Log.i(TAG, "MNN loaded")
            System.loadLibrary("MNN_Express"); Log.i(TAG, "MNN_Express loaded")
            System.loadLibrary("llm"); Log.i(TAG, "llm loaded")
            try { System.loadLibrary("MNN_CL") } catch (_: UnsatisfiedLinkError) { }
            try { System.loadLibrary("MNN_Vulkan") } catch (_: UnsatisfiedLinkError) { }
            System.loadLibrary("mnn_llm_bridge"); Log.i(TAG, "mnn_llm_bridge loaded")
            nativeLoaded = true
            Log.i(TAG, "All MNN libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "FAILED to load native libraries: ${e.message}", e)
        }
    }

    /**
     * Creates a native MNN LLM instance from model files in the given directory.
     *
     * @param modelDir Absolute path to directory containing llm.mnn, llm.mnn.weight, etc.
     * @param threadNum Number of CPU threads to use for inference
     * @param temperature Sampling temperature
     * @param topP Top-p sampling threshold
     * @param topK Top-k sampling limit
     * @param repetitionPenalty Repetition penalty
     * @param maxNewTokens Maximum number of tokens to generate
     * @return Native handle (>0) on success, 0 on failure
     */
    external fun nativeCreateLlm(
        modelDir: String, threadNum: Int,
        temperature: Float, topP: Float, topK: Int, repetitionPenalty: Float, maxNewTokens: Int
    ): Long

    /**
     * Runs inference on an image file using the LLM.
     *
     * @param nativeHandle Handle returned by nativeCreateLlm
     * @param imageData In-memory JPEG image byte array
     * @return LLM response string (JSON food identification)
     */
    external fun nativeRunInference(nativeHandle: Long, imageData: ByteArray): String

    external fun nativeRunInferenceWithHint(nativeHandle: Long, imageData: ByteArray, hint: String): String

    /**
     * Destroys the native LLM instance and releases all resources.
     *
     * @param nativeHandle Handle returned by nativeCreateLlm
     */
    external fun nativeDestroyLlm(nativeHandle: Long)
}
