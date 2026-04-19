package com.example.foodexpiryapp.inference.mnn

import com.taobao.android.mnn.MNNForwardType

data class MnnLlmConfig(
    val modelDirPath: String = "",
    val backendType: Int = MNNForwardType.FORWARD_CPU.type,
    val threadNum: Int = 4,
    val useMmap: Boolean = true,
    val chunkSize: Int = 128,
    val maxNewTokens: Int = 256,
    val temperature: Float = 0.15f,
    val topP: Float = 0.85f,
    val topK: Int = 5,
    val repetitionPenalty: Float = 1.15f
) {
    companion object {
        fun createOptimal(): MnnLlmConfig {
            val availableCores = Runtime.getRuntime().availableProcessors()
            val threadCount = minOf(availableCores, 4).coerceAtLeast(1)
            return MnnLlmConfig(threadNum = threadCount)
        }
    }
}
