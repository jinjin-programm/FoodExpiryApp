package com.example.foodexpiryapp.domain.model

/**
 * Model file manifest with SHA-256 hashes.
 * Per DL-03: Hashes used for integrity verification.
 * These values come from HuggingFace repo metadata.
 */
data class ModelManifest(
    val version: String = "1.0.0",
    val modelId: String = "jinjin06/Qwen3.5-2B-MNN",
    val files: List<ModelFile> = emptyList()
)

/**
 * A single model file entry in the manifest.
 *
 * @param relativePath Path within the model directory (e.g., "llm.mnn.weight")
 * @param expectedSha256 SHA-256 hex string for integrity verification. Empty if unknown.
 * @param estimatedSizeBytes Estimated file size for progress display before download starts.
 */
data class ModelFile(
    val relativePath: String,
    val expectedSha256: String = "",
    val estimatedSizeBytes: Long = 0
)
