package com.example.foodexpiryapp.inference.yolo

import android.util.Log

/**
 * JNI native method declarations for MNN YOLO C++ bridge.
 * Methods are implemented in mnn_yolo_bridge.cpp.
 *
 * Per D-01: Custom JNI bridge pattern (same as MnnLlmNative) for full control over YOLO inference.
 * Per D-04: Follows the same loadLibrary pattern as MnnLlmNative.
 */
object MnnYoloNative {
    private const val TAG = "MnnYoloNative"

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
            try { System.loadLibrary("MNN_CL") } catch (_: UnsatisfiedLinkError) { }
            try { System.loadLibrary("MNN_Vulkan") } catch (_: UnsatisfiedLinkError) { }
            System.loadLibrary("mnn_yolo_bridge"); Log.i(TAG, "mnn_yolo_bridge loaded")
            nativeLoaded = true
            Log.i(TAG, "All YOLO native libraries loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "FAILED to load YOLO native libraries: ${e.message}", e)
        }
    }

    /**
     * Creates a native MNN YOLO instance from a model file in assets.
     *
     * @param modelAssetPath Path to MNN-format YOLO model in APK assets (e.g., "yolo_food.mnn")
     * @param threadNum Number of CPU threads to use for inference
     * @return Native handle (>0) on success, 0 on failure
     */
    external fun nativeCreateYolo(modelAssetPath: String, threadNum: Int): Long

    /**
     * Runs YOLO detection on bitmap image data.
     *
     * @param nativeHandle Handle returned by nativeCreateYolo
     * @param bitmapData JPEG-encoded byte array of the input image
     * @param width Original image width in pixels
     * @param height Original image height in pixels
     * @return FloatArray of detections: [x1,y1,x2,y2,confidence,class_id,...] per row
     *         Per D-05: Output format matches MnnYoloPostprocessor.parseDetections (6+ values per row)
     */
    external fun nativeRunDetection(nativeHandle: Long, bitmapData: ByteArray, width: Int, height: Int): FloatArray

    /**
     * Destroys the native YOLO instance and releases all resources.
     *
     * @param nativeHandle Handle returned by nativeCreateYolo
     */
    external fun nativeDestroyYolo(nativeHandle: Long)
}
