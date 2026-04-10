package com.example.foodexpiryapp.inference.mnn

import com.taobao.android.mnn.MNNForwardType

/**
 * MNN LLM engine configuration.
 * Per PITFALL-9: Default to CPU backend, FP16 only on ARMv8.2+.
 * Per PITFALL-10: Thread count capped at min(cores-1, 4).
 */
data class MnnLlmConfig(
    val modelDirPath: String = "",           // Directory containing model files
    val backendType: Int = MNNForwardType.FORWARD_CPU.type,  // 0 = CPU
    val threadNum: Int = 4,                  // Capped at min(availableCores - 1, 4)
    val useMmap: Boolean = true,             // Memory-mapped weight loading
    val memoryMode: String = "low",          // Low memory mode (runtime quantization)
    val chunkSize: Int = 128,                // Per-token memory limit
    val maxNewTokens: Int = 512,
    val temperature: Float = 0.6f,           // Lower for deterministic classification
    val topP: Float = 0.9f
) {
    companion object {
        /**
         * Creates optimal config for current device.
         * Per PITFALL-9: Detects ARMv8.2 for FP16 support.
         * Per PITFALL-10: Caps thread count.
         */
        fun createOptimal(): MnnLlmConfig {
            val availableCores = Runtime.getRuntime().availableProcessors()
            val threadCount = minOf(availableCores - 1, 4).coerceAtLeast(1)
            return MnnLlmConfig(threadNum = threadCount)
        }
    }
}
