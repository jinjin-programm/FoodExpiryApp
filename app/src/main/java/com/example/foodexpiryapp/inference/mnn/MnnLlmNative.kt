package com.example.foodexpiryapp.inference.mnn

import android.util.Log

/**
 * JNI native method declarations for MNN LLM C++ bridge.
 * Methods are implemented in mnn_llm_bridge.cpp.
 * Reference: MNN apps/Android/MnnLlmChat
 */
object MnnLlmNative {
    private const val TAG = "MnnLlmNative"

    init {
        try {
            System.loadLibrary("c++_shared")
            System.loadLibrary("MNN")
            System.loadLibrary("MNN_Express")
            System.loadLibrary("llm")
            // Optional: GPU backends
            try { System.loadLibrary("MNN_CL") } catch (e: UnsatisfiedLinkError) { Log.d(TAG, "MNN_CL not available (OpenCL)") }
            try { System.loadLibrary("MNN_Vulkan") } catch (e: UnsatisfiedLinkError) { Log.d(TAG, "MNN_Vulkan not available") }
            // Load our JNI bridge last
            System.loadLibrary("mnn_llm_bridge")
            Log.i(TAG, "All MNN libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "MNN libraries not found — LLM native features unavailable", e)
        }
    }

    /**
     * Creates a native MNN LLM instance from model files in the given directory.
     *
     * @param modelDir Absolute path to directory containing llm.mnn, llm.mnn.weight, etc.
     * @param threadNum Number of CPU threads to use for inference
     * @param memoryMode Memory mode: "low" for runtime quantization, "high" for fp32
     * @return Native handle (>0) on success, 0 on failure
     */
    external fun nativeCreateLlm(modelDir: String, threadNum: Int, memoryMode: String): Long

    /**
     * Runs inference on image data using the LLM.
     *
     * @param nativeHandle Handle returned by nativeCreateLlm
     * @param imageData JPEG-compressed image bytes
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return LLM response string (JSON food identification)
     */
    external fun nativeRunInference(nativeHandle: Long, imageData: ByteArray, width: Int, height: Int): String

    /**
     * Destroys the native LLM instance and releases all resources.
     *
     * @param nativeHandle Handle returned by nativeCreateLlm
     */
    external fun nativeDestroyLlm(nativeHandle: Long)
}
