package com.example.foodexpiryapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP Range-based resumable download service for HuggingFace model files.
 *
 * Per DL-02: Supports resume after interruption via HTTP Range headers.
 * Per DL-03: SHA-256 checksum verification before marking model ready.
 * Per DL-04: Downloads to .part files; caller renames after verification.
 *
 * Per PITFALL-12: Always resumes from original HuggingFace URL (not cached redirect).
 * Per PITFALL-4: Use .part file pattern to avoid corrupting final model files.
 */
@Singleton
class HuggingFaceDownloadService @Inject constructor(
    private val hfTokenProvider: HfTokenProvider
) {

    companion object {
        const val HF_REPO = "taobao-mnn/Qwen3.5-2B-MNN"
        const val HF_BASE_URL = "https://huggingface.co"
        const val USER_AGENT = "FoodExpiryApp/2.0"
        const val BUFFER_SIZE = 8192
        const val PROGRESS_INTERVAL_MS = 500L

        /** Model files to download from HuggingFace repo */
        val MODEL_FILES = listOf(
            "llm.mnn",
            "llm.mnn.weight",
            "llm_config.json",
            "config.json",
            "tokenizer.txt",
            "visual.mnn"
        )
    }

    /**
     * Applies Authorization header for private repo access if a token is available.
     */
    private fun applyAuth(connection: HttpURLConnection) {
        val token = hfTokenProvider.getToken()
        if (token.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
    }

    /**
     * Downloads a file from HuggingFace with HTTP Range resume support.
     *
     * @param relativePath Path within repo (e.g., "llm.mnn.weight")
     * @param partFile Target .part file to write to
     * @param startByte Byte offset to resume from (0 for fresh download)
     * @param expectedSha256 Expected SHA-256 hash for verification (null if unknown)
     * @return Flow emitting [DownloadProgress] updates (bytes, percentage, speed)
     * @throws [DownloadException] on HTTP errors or I/O failures
     */
    fun downloadWithResume(
        relativePath: String,
        partFile: File,
        startByte: Long = 0L,
        expectedSha256: String? = null
    ): Flow<DownloadProgress> = flow {
        // Build URL — always use original HF URL per PITFALL-12 (signed CDN tokens may expire)
        val url = "$HF_BASE_URL/$HF_REPO/resolve/main/$relativePath"

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", USER_AGENT)
        applyAuth(connection)

        // Set Range header for resume
        if (startByte > 0) {
            connection.setRequestProperty("Range", "bytes=$startByte-")
        }

        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
            val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: ""
            throw DownloadException("HTTP $responseCode: ${connection.responseMessage} — $errorBody")
        }

        // Determine total file size
        val totalBytes = parseContentRangeTotal(connection) ?: connection.contentLengthLong
        val isResuming = responseCode == HttpURLConnection.HTTP_PARTIAL
        var accumulatedBytes = if (isResuming) startByte else 0L

        // Ensure parent directory exists
        partFile.parentFile?.mkdirs()

        // Stream response body to .part file in append mode (for resume)
        connection.inputStream.use { input ->
            FileOutputStream(partFile, isResuming).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var lastProgressTime = System.currentTimeMillis()
                var bytesSinceLastProgress = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    accumulatedBytes += bytesRead.toLong()
                    bytesSinceLastProgress += bytesRead.toLong()

                    // Emit progress every 500ms
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= PROGRESS_INTERVAL_MS) {
                        val elapsed = now - lastProgressTime
                        val speed = if (elapsed > 0) bytesSinceLastProgress * 1000 / elapsed else 0L
                        val percentage = if (totalBytes > 0) (accumulatedBytes * 100 / totalBytes).toInt() else 0
                        emit(
                            DownloadProgress(
                                downloadedBytes = accumulatedBytes,
                                totalBytes = totalBytes,
                                percentage = percentage,
                                speedBytesPerSec = speed
                            )
                        )
                        lastProgressTime = now
                        bytesSinceLastProgress = 0L
                    }
                }
            }
        }

        // Final progress emission — download complete
        emit(
            DownloadProgress(
                downloadedBytes = accumulatedBytes,
                totalBytes = totalBytes,
                percentage = 100,
                speedBytesPerSec = 0
            )
        )
    }.flowOn(Dispatchers.IO)

    /**
     * Verifies SHA-256 checksum of a downloaded file.
     * Per DL-03: Model files must be verified before use.
     *
     * @param file The file to verify
     * @param expectedHash The expected SHA-256 hex string (lowercase)
     * @return true if the file's SHA-256 matches the expected hash
     */
    fun verifySha256(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash == expectedHash.lowercase()
    }

    /**
     * Computes the SHA-256 hash of a file.
     *
     * @param file The file to hash
     * @return The SHA-256 hex string (lowercase), or null if the file doesn't exist
     */
    fun computeSha256(file: File): String? {
        if (!file.exists()) return null
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Parses total file size from Content-Range header.
     * Format: "bytes 0-1234567/1234568"
     */
    private fun parseContentRangeTotal(connection: HttpURLConnection): Long? {
        val contentRange = connection.getHeaderField("Content-Range")
        if (contentRange == null) return null
        val parts = contentRange.split("/")
        return parts.lastOrNull()?.toLongOrNull()
    }
}

/**
 * Progress data emitted during a model file download.
 */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percentage: Int,
    val speedBytesPerSec: Long
) {
    /** Estimated time remaining in seconds (0 if unknown) */
    val etaSeconds: Long = if (speedBytesPerSec > 0 && totalBytes > downloadedBytes) {
        (totalBytes - downloadedBytes) / speedBytesPerSec
    } else 0L

    /** Downloaded bytes in megabytes */
    val downloadedMB: Double = downloadedBytes / (1024.0 * 1024.0)

    /** Total bytes in megabytes */
    val totalMB: Double = totalBytes / (1024.0 * 1024.0)

    /** Download speed in megabytes per second */
    val speedMBPerSec: Double = speedBytesPerSec / (1024.0 * 1024.0)

    /** Whether the download is complete */
    val isComplete: Boolean = percentage >= 100
}

/**
 * Exception thrown when a download fails.
 */
class DownloadException(message: String) : Exception(message)
