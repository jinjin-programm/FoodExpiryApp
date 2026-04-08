package com.example.foodexpiryapp.inference.mnn

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages model lifecycle with mutual exclusion.
 * Per MNN-05: Only one heavy model loaded at a time.
 * Per PITFALL-1: Prevents OOM from simultaneous YOLO + LLM.
 */
@Singleton
class ModelLifecycleManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ModelLifecycleManager"
        private const val MIN_AVAILABLE_MEMORY_MB = 2048L // 2GB minimum
    }

    enum class ModelType { YOLO, LLM }

    private val mutex = Mutex()
    private var activeModel: ModelType? = null

    /**
     * Checks if there's enough memory to load a model.
     * Per PITFALL-1: Reject if <2GB available.
     */
    fun hasEnoughMemory(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val availMB = memoryInfo.availMem / (1024 * 1024)
        Log.d(TAG, "Available memory: ${availMB}MB (need ${MIN_AVAILABLE_MEMORY_MB}MB)")
        return availMB >= MIN_AVAILABLE_MEMORY_MB
    }

    /**
     * Acquires exclusive access to load a model.
     * Per MNN-05: Enforces mutual exclusion.
     *
     * @param modelType Which model wants to load
     * @return true if acquired, false if another model is active
     */
    suspend fun acquire(modelType: ModelType): Boolean = mutex.withLock {
        if (activeModel != null && activeModel != modelType) {
            Log.w(TAG, "Cannot acquire $modelType — $activeModel is active")
            return false
        }

        if (!hasEnoughMemory()) {
            Log.w(TAG, "Cannot acquire $modelType — insufficient memory")
            return false
        }

        activeModel = modelType
        Log.d(TAG, "Acquired $modelType")
        return true
    }

    /**
     * Releases the active model.
     */
    suspend fun release(modelType: ModelType) = mutex.withLock {
        if (activeModel == modelType) {
            activeModel = null
            Log.d(TAG, "Released $modelType")
        }
    }

    /**
     * Gets the currently active model type.
     */
    fun getActiveModel(): ModelType? = activeModel

    /**
     * Force releases all models (e.g., on app backgrounding).
     */
    suspend fun releaseAll() = mutex.withLock {
        activeModel = null
        Log.d(TAG, "Released all models")
    }
}
