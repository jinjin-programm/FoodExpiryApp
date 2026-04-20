package com.example.foodexpiryapp.inference.mnn

import com.example.foodexpiryapp.util.AppLog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto-cleans up LLM engine on app backgrounding.
 * Per MNN-05: Releases native resources when app goes to background.
 * Per PITFALL-1: Frees ~1.5-2GB of mmap weight memory on backgrounding.
 *
 * Register with ProcessLifecycleOwner in the Application class or Hilt module.
 */
@Singleton
class ProcessLifecycleObserver @Inject constructor(
    private val engine: MnnLlmEngine
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ProcessLifecycleObserver"
    }

    // Dedicated scope for lifecycle callbacks — survives app lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStop(owner: LifecycleOwner) {
        AppLog.d(TAG, "App backgrounded — unloading LLM model to free memory")
        if (engine.isModelLoaded()) {
            scope.launch {
                engine.unloadModel()
            }
        }
    }
}
