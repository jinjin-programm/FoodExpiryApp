package com.example.foodexpiryapp.presentation.ui.llm

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import kotlin.math.max

class LlamaBridge private constructor() {

    data class ModelConfig(
        val contextSize: Int = 1024,
        val threads: Int = defaultThreadCount(),
    )

    data class LoadReport(
        val success: Boolean,
        val modelPath: String,
        val mmprojPath: String,
        val modelSizeBytes: Long,
        val mmprojSizeBytes: Long,
        val loadTimeMs: Long,
        val contextSize: Int,
        val threads: Int,
    )

    data class GenerationReport(
        val response: String,
        val pixelPackingMs: Long,
        val nativeInferenceMs: Long,
        val totalMs: Long,
        val approxTokensPerSecond: Double,
    )

    companion object {
        private const val TAG = "LlamaBridge"
        private const val MODEL_DIR = "llm"
        private const val MODEL_FILE = "model.gguf"
        private const val MMPROJ_FILE = "mmproj.gguf"

        @Volatile
        private var instance: LlamaBridge? = null

        fun getInstance(): LlamaBridge {
            return instance ?: synchronized(this) {
                instance ?: LlamaBridge().also { instance = it }
            }
        }

        fun defaultThreadCount(): Int {
            val available = Runtime.getRuntime().availableProcessors()
            return (available - 1).coerceIn(2, 6)
        }

        fun recommendedVisionConfig(): ModelConfig {
            return ModelConfig(
                contextSize = 1024,
                threads = defaultThreadCount(),
            )
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

    @Volatile
    private var loadedConfig: ModelConfig? = null

    private val stateLock = Any()

    fun logDeviceProfile(context: Context) {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)

        val totalMemMb = memInfo.totalMem / (1024 * 1024)
        val availMemMb = memInfo.availMem / (1024 * 1024)
        val abi = Build.SUPPORTED_ABIS.joinToString()

        Log.i(
            TAG,
            "Device profile: cores=${Runtime.getRuntime().availableProcessors()}, abi=$abi, android=${Build.VERSION.SDK_INT}, ram_total_mb=$totalMemMb, ram_avail_mb=$availMemMb"
        )
    }

    fun ensureModelLoaded(context: Context, config: ModelConfig = ModelConfig()): LoadReport {
        synchronized(stateLock) {
            if (isModelLoaded && hasVision && loadedConfig == config) {
                val modelFile = File(context.filesDir, "$MODEL_DIR/$MODEL_FILE")
                val mmprojFile = File(context.filesDir, "$MODEL_DIR/$MMPROJ_FILE")
                return LoadReport(
                    success = true,
                    modelPath = modelFile.absolutePath,
                    mmprojPath = mmprojFile.absolutePath,
                    modelSizeBytes = modelFile.length(),
                    mmprojSizeBytes = mmprojFile.length(),
                    loadTimeMs = 0L,
                    contextSize = config.contextSize,
                    threads = config.threads,
                )
            }
        }

        val modelDir = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelDir, MODEL_FILE)
        val mmprojFile = File(modelDir, MMPROJ_FILE)

        val loadStart = System.currentTimeMillis()
        copyAssetIfMissing(context, "$MODEL_DIR/$MODEL_FILE", modelFile)
        copyAssetIfMissing(context, "$MODEL_DIR/$MMPROJ_FILE", mmprojFile)

        val loadSuccess = synchronized(stateLock) {
            if (!isModelLoaded || loadedConfig != config) {
                if (isModelLoaded || hasVision) {
                    freeModelLocked()
                }

                val loaded = loadModel(modelFile.absolutePath, config.contextSize, config.threads)
                if (!loaded) {
                    false
                } else {
                    val mmprojLoaded = loadMmproj(mmprojFile.absolutePath)
                    if (mmprojLoaded) {
                        loadedConfig = config
                    }
                    mmprojLoaded
                }
            } else {
                true
            }
        }

        val loadMs = System.currentTimeMillis() - loadStart
        Log.i(
            TAG,
            "Model ready=$loadSuccess in ${loadMs}ms, model_mb=${modelFile.length() / (1024 * 1024)}, mmproj_mb=${mmprojFile.length() / (1024 * 1024)}, context=${config.contextSize}, threads=${config.threads}"
        )

        return LoadReport(
            success = loadSuccess,
            modelPath = modelFile.absolutePath,
            mmprojPath = mmprojFile.absolutePath,
            modelSizeBytes = modelFile.length(),
            mmprojSizeBytes = mmprojFile.length(),
            loadTimeMs = loadMs,
            contextSize = config.contextSize,
            threads = config.threads,
        )
    }

    private fun copyAssetIfMissing(context: Context, assetPath: String, target: File) {
        if (target.exists()) return

        target.parentFile?.mkdirs()
        val start = System.currentTimeMillis()
        context.assets.open(assetPath).use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val elapsed = System.currentTimeMillis() - start
        Log.i(TAG, "Copied $assetPath to ${target.absolutePath} in ${elapsed}ms")
    }

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

            val start = System.currentTimeMillis()
            val result = nativeLoadModel(modelPath, contextSize, optimalThreads)
            isModelLoaded = result == 0

            if (isModelLoaded) {
                val elapsed = System.currentTimeMillis() - start
                Log.i(TAG, "Model loaded successfully in ${elapsed}ms")
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
        synchronized(stateLock) {
            freeModelLocked()
        }
    }

    private fun freeModelLocked() {
        try {
            nativeFreeModel()
        } catch (e: Exception) {
            Log.e(TAG, "Exception freeing model: ${e.message}")
        } finally {
            isModelLoaded = false
            hasVision = false
            loadedConfig = null
            Log.i(TAG, "Model freed")
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

            val start = System.currentTimeMillis()
            val result = nativeLoadMmproj(mmprojPath)
            hasVision = result == 0

            if (hasVision) {
                val elapsed = System.currentTimeMillis() - start
                Log.i(TAG, "Mmproj loaded successfully in ${elapsed}ms, vision enabled")
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

    fun generateWithImageDetailed(prompt: String, bitmap: Bitmap, maxTokens: Int = 96): GenerationReport {
        if (!isModelLoaded || !hasVision) {
            val error = if (!isModelLoaded) "Error: Model not loaded" else "Error: Vision not loaded"
            return GenerationReport(error, 0L, 0L, 0L, 0.0)
        }

        return try {
            val totalStart = System.currentTimeMillis()

            val pixelStart = System.currentTimeMillis()
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val rgbData = ByteArray(width * height * 3)
            for (i in pixels.indices) {
                val p = pixels[i]
                rgbData[i * 3] = ((p shr 16) and 0xff).toByte()
                rgbData[i * 3 + 1] = ((p shr 8) and 0xff).toByte()
                rgbData[i * 3 + 2] = (p and 0xff).toByte()
            }
            val pixelMs = System.currentTimeMillis() - pixelStart

            val nativeStart = System.currentTimeMillis()
            val response = nativeGenerateWithImage(prompt, rgbData, width, height, maxTokens)
            val nativeMs = System.currentTimeMillis() - nativeStart

            val totalMs = System.currentTimeMillis() - totalStart
            val approxTokens = max(1, response.trim().split(Regex("\\s+")).size)
            val tps = if (nativeMs > 0) (approxTokens * 1000.0) / nativeMs else 0.0

            Log.i(
                TAG,
                "Vision generation done: total=${totalMs}ms, pixel_pack=${pixelMs}ms, native=${nativeMs}ms, approx_tps=${"%.2f".format(tps)}"
            )

            GenerationReport(
                response = response,
                pixelPackingMs = pixelMs,
                nativeInferenceMs = nativeMs,
                totalMs = totalMs,
                approxTokensPerSecond = tps,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Vision generation error: ${e.message}", e)
            GenerationReport("Error: ${e.message}", 0L, 0L, 0L, 0.0)
        }
    }

    fun generateWithImage(prompt: String, bitmap: Bitmap, maxTokens: Int = 256): String {
        return generateWithImageDetailed(prompt, bitmap, maxTokens).response
    }

    private external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): Int
    private external fun nativeFreeModel()
    private external fun nativeIsLoaded(): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeLoadMmproj(mmprojPath: String): Int
    private external fun nativeGenerateWithImage(prompt: String, rgbData: ByteArray, width: Int, height: Int, maxTokens: Int): String
}
