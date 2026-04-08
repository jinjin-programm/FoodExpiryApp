package com.example.foodexpiryapp.domain.model

/**
 * UI state for download progress display.
 * Per DL-05: Shows percentage and ETA.
 * Per DL-06: WiFi check state.
 */
sealed class DownloadUiState {
    /** Model already downloaded and verified. */
    object Ready : DownloadUiState()

    /** No download has started. */
    object NotDownloaded : DownloadUiState()

    /** WiFi check required before downloading over cellular. */
    data class WifiCheckRequired(val estimatedSizeMB: Double) : DownloadUiState()

    /** Download in progress with progress details. */
    data class Downloading(
        val currentFile: String,       // Name of file being downloaded
        val fileProgress: Int,         // 0-100 for current file
        val overallProgress: Int,      // 0-100 across all files
        val downloadedMB: Double,
        val totalMB: Double,
        val speedMBPerSec: Double,
        val etaSeconds: Long           // Estimated time remaining
    ) : DownloadUiState()

    /** Verifying downloaded files. */
    data class Verifying(val currentFile: String) : DownloadUiState()

    /** Download completed successfully. */
    object Complete : DownloadUiState()

    /** Download failed with error details. */
    data class Error(val message: String, val canRetry: Boolean) : DownloadUiState()

    /** Download paused (e.g., waiting for WiFi). */
    object Paused : DownloadUiState()
}
