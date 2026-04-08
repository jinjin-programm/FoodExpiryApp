package com.example.foodexpiryapp.domain.model

/**
 * Observable states of the LLM model lifecycle.
 * UI observes this via [com.example.foodexpiryapp.domain.repository.LlmInferenceRepository].
 */
sealed class ModelState {
    /** Model has not been downloaded yet. */
    object NotDownloaded : ModelState()

    /** Model files are being downloaded. */
    data class Downloading(val progress: Int, val currentFile: String) : ModelState()

    /** Downloaded files are being verified (SHA-256). */
    data class Verifying(val currentFile: String) : ModelState()

    /** WiFi check required before downloading over cellular. */
    data class WifiCheckRequired(val estimatedSizeMB: Double) : ModelState()

    /** Model is loading into memory. */
    object Loading : ModelState()

    /** Model is ready for inference. */
    object Ready : ModelState()

    /** An error occurred (download, load, or inference). */
    data class Error(val message: String) : ModelState()
}
