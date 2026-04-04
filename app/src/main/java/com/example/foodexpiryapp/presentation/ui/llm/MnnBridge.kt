package com.example.foodexpiryapp.presentation.ui.llm

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class MnnBridge {

    data class MnnConfig(
        val backendType: String = "opencl",
        val threadNum: Int = 68,
        val precision: String = "low",
        val memory: String = "low",
        val maxNewTokens: Int = 96,
    )

    data class GenerationReport(
        val response: String,
        val pixelPackingMs: Long,
        val nativeInferenceMs: Long,
        val totalMs: Long,
        val approxTokensPerSecond: Double,
    )

    companion object {
        private const val TAG = "MnnBridge"
        private const val MODEL_DIR = "mnn/qwen3-vl-2b"
        private const val TARGET_IMAGE_SIZE = 420 // MNN Qwen3.5-VL expects 420x420

        init {
            System.loadLibrary("MNN")
            System.loadLibrary("MNN_Express")
            System.loadLibrary("MNN_CL")
            System.loadLibrary("MNNOpenCV")
            System.loadLibrary("llm")
            System.loadLibrary("mnn_jni")
            Log.i(TAG, "MNN native libraries loaded")
        }
    }

    private external fun nativeLoadModel(configPath: String): Boolean
    private external fun nativeGenerateWithImage(
        prompt: String, rgbData: ByteArray,
        width: Int, height: Int, maxTokens: Int
    ): String
    private external fun nativeGenerateText(prompt: String, maxTokens: Int): String
    private external fun nativeReset()
    private external fun nativeRelease()

    private var isLoaded = false

    fun loadModel(context: Context, modelDir: String = MODEL_DIR): Boolean {
        if (isLoaded) return true

        val startTime = System.currentTimeMillis()
        val destDir = File(context.filesDir, modelDir)

        // Always force re-copy to ensure config.json and tokenizer are in sync
        if (destDir.exists()) {
            Log.d(TAG, "Cleaning old model directory for fresh copy")
            destDir.deleteRecursively()
        }
        Log.d(TAG, "Copying MNN model assets to $destDir")
        copyAssets(context, modelDir, destDir)

        val configPath = File(destDir, "config.json").absolutePath
        Log.d(TAG, "Loading MNN model from: $configPath")

        isLoaded = nativeLoadModel(configPath)
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Model load ${if (isLoaded) "success" else "failed"} in ${elapsed}ms")

        return isLoaded
    }

    fun generateWithImageDetailed(prompt: String, bitmap: Bitmap, maxTokens: Int = 96): GenerationReport {
        val startTime = System.currentTimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, TARGET_IMAGE_SIZE, TARGET_IMAGE_SIZE, true)
        val pixels = getBitmapPixels(resizedBitmap)
        val pixelTime = System.currentTimeMillis() - startTime

        Log.d(TAG, "Running MNN inference on image size: ${resizedBitmap.width}x${resizedBitmap.height}")

        val response = nativeGenerateWithImage(
            prompt, pixels,
            resizedBitmap.width, resizedBitmap.height, maxTokens
        )

        val inferenceTime = System.currentTimeMillis() - startTime - pixelTime
        val totalMs = System.currentTimeMillis() - startTime

        val tokenCount = response.split(Regex("\\s+")).size
        val tps = if (totalMs > 0) tokenCount.toDouble() / (totalMs / 1000.0) else 0.0

        Log.d(TAG, "Total time: ${totalMs}ms, preprocess=${pixelTime}ms, native=${inferenceTime}ms, approx_tps=${"%.2f".format(tps)}")
        Log.d(TAG, "Raw Response: $response")

        return GenerationReport(
            response = response,
            pixelPackingMs = pixelTime,
            nativeInferenceMs = inferenceTime,
            totalMs = totalMs,
            approxTokensPerSecond = tps
        )
    }

    fun generateText(prompt: String, maxTokens: Int = 256): String {
        return nativeGenerateText(prompt, maxTokens)
    }

    fun reset() {
        nativeReset()
    }

    fun release() {
        nativeRelease()
        isLoaded = false
    }

    private fun getBitmapPixels(bitmap: Bitmap): ByteArray {
        val pixels = ByteArray(bitmap.width * bitmap.height * 3)
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in intArray.indices) {
            val pixel = intArray[i]
            pixels[i * 3] = ((pixel shr 16) and 0xFF).toByte()
            pixels[i * 3 + 1] = ((pixel shr 8) and 0xFF).toByte()
            pixels[i * 3 + 2] = (pixel and 0xFF).toByte()
        }
        return pixels
    }

    private fun copyAssets(context: Context, assetDir: String, destDir: File) {
        destDir.mkdirs()
        val assetManager = context.assets
        try {
            val files = assetManager.list(assetDir)
            if (!files.isNullOrEmpty()) {
                for (filename in files) {
                    val assetPath = "$assetDir/$filename"
                    val destFile = File(destDir, filename)
                    val subFiles = assetManager.list(assetPath)
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        copyAssets(context, assetPath, destFile)
                    } else {
                        try {
                            assetManager.open(assetPath).use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to copy asset file $assetPath", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy assets", e)
        }
    }
}
