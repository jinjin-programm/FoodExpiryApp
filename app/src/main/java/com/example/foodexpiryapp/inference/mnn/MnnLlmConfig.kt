package com.example.foodexpiryapp.inference.mnn

import com.taobao.android.mnn.MNNForwardType

data class MnnLlmConfig(
    val modelDirPath: String = "",
    val backendType: Int = MNNForwardType.FORWARD_CPU.type,
    val threadNum: Int = 8,
    val useMmap: Boolean = true,
    val memoryMode: String = "low",
    val precision: String = "high",
    val chunkSize: Int = 128,
    val maxNewTokens: Int = 512,
    val temperature: Float = 0.6f,
    val topP: Float = 0.9f
) {
    companion object {
        fun createOptimal(): MnnLlmConfig {
            val availableCores = Runtime.getRuntime().availableProcessors()
            val threadCount = minOf(availableCores, 8).coerceAtLeast(1)
            return MnnLlmConfig(threadNum = threadCount)
        }
    }
}
