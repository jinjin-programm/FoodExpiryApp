package com.example.foodexpiryapp.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting download state across app restarts.
 * Per DL-07: Download state (bytes downloaded, ETag, status) must survive process death.
 *
 * Each row tracks the download progress of a single model file.
 */
@Entity(tableName = "download_state")
data class DownloadStateEntity(
    /**
     * Primary key: relative path of the file within the model directory.
     * E.g., "llm.mnn.weight"
     */
    @PrimaryKey
    val filePath: String,

    /** Total file size in bytes (from Content-Range or Content-Length header) */
    val totalBytes: Long = 0L,

    /** Bytes successfully downloaded and written to .part file */
    val downloadedBytes: Long = 0L,

    /**
     * ETag from the HuggingFace response header.
     * Per PITFALL-12: Cache ETag (not redirect URL) for resume validation.
     * If ETag changes between sessions, the file may have been updated on HuggingFace.
     */
    val eTag: String? = null,

    /** Last-Modified header from the HuggingFace response */
    val lastModified: String? = null,

    /**
     * Download status.
     * Possible values: PENDING, DOWNLOADING, VERIFYING, COMPLETE, FAILED, CANCELLED
     */
    val status: String = "PENDING",

    /** Error message if status is FAILED */
    val errorMessage: String? = null,

    /**
     * Expected SHA-256 hash of the file.
     * Per DL-03: Model files must be verified against this hash before use.
     */
    val expectedSha256: String? = null,

    /** Actual SHA-256 hash computed after download completes */
    val actualSha256: String? = null,

    /** Timestamp of the last state update */
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_VERIFYING = "VERIFYING"
        const val STATUS_COMPLETE = "COMPLETE"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
    }

    /** Whether the download can be resumed from the current byte offset */
    val canResume: Boolean
        get() = status == STATUS_DOWNLOADING && downloadedBytes > 0
}
