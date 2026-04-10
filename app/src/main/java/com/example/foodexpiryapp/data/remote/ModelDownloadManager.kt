package com.example.foodexpiryapp.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.foodexpiryapp.data.local.ModelStorageManager
import com.example.foodexpiryapp.data.local.dao.DownloadStateDao
import com.example.foodexpiryapp.data.local.database.DownloadStateEntity
import com.example.foodexpiryapp.domain.model.DownloadUiState
import com.example.foodexpiryapp.domain.model.ModelFile
import com.example.foodexpiryapp.domain.model.ModelManifest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates downloading all model files from HuggingFace.
 *
 * Per DL-01: Downloads model files via [HuggingFaceDownloadService].
 * Per DL-05: Emits download progress (percentage + ETA) via [Flow].
 * Per DL-06: Enforces WiFi check before downloading over cellular.
 * Per DL-07: Persists download state in Room; resumes after app kill.
 * Per DL-03: Verifies SHA-256 after each file download.
 * Per DL-04: Uses atomic rename via [ModelStorageManager].
 * Per T-05-07: Rejects model files on SHA-256 mismatch (tampering mitigation).
 * Per T-05-08: Mutex + AtomicBoolean prevent concurrent downloads (DoS mitigation).
 */
@Singleton
class ModelDownloadManager @Inject constructor(
    private val downloadService: HuggingFaceDownloadService,
    private val storageManager: ModelStorageManager,
    private val downloadStateDao: DownloadStateDao,
    private val hfTokenProvider: HfTokenProvider,
    @ApplicationContext private val context: Context
) {
    private val downloadMutex = Mutex()
    private val isDownloading = AtomicBoolean(false)
    private var currentJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "ModelDownloadManager"

        /** Default manifest — Qwen3.5-2B-MNN model. */
        private val DEFAULT_MANIFEST = ModelManifest(
            version = "1.0.0",
            modelId = "taobao-mnn/Qwen3.5-2B-MNN",
            files = listOf(
                ModelFile("config.json", estimatedSizeBytes = 512),
                ModelFile("llm_config.json", estimatedSizeBytes = 2_048),
                ModelFile("tokenizer.txt", estimatedSizeBytes = 500_000),
                ModelFile("visual.mnn", estimatedSizeBytes = 50_000_000),
                ModelFile("visual.mnn.weight", estimatedSizeBytes = 400_000_000),
                ModelFile("llm.mnn", estimatedSizeBytes = 50_000_000),
                ModelFile("llm.mnn.weight", estimatedSizeBytes = 1_200_000_000)
            )
        )

        /** Resolved manifest with SHA-256 hashes from HuggingFace API. */
        @Volatile
        private var resolvedManifest: ModelManifest = DEFAULT_MANIFEST
    }

    /**
     * Fetches SHA-256 hashes from HuggingFace API for model files.
     * Per DL-03: SHA-256 verification requires known-good hashes.
     *
     * API: GET https://huggingface.co/api/models/jinjin06/Qwen3.5-2B-MNN
     * Response includes siblings[].lfs.sha256 for LFS-tracked files.
     * Non-LFS files (config.json, etc.) are small — verification optional.
     */
    private suspend fun fetchSha256Hashes(): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val request = URL("https://huggingface.co/api/models/${DEFAULT_MANIFEST.modelId}")
            val connection = request.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", HuggingFaceDownloadService.USER_AGENT)

            // Add Authorization header for private repo access
            val token = hfTokenProvider.getToken()
            if (token.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Failed to fetch SHA-256 hashes: HTTP $responseCode")
                return@withContext emptyMap()
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json = org.json.JSONObject(body)
            val siblings = json.getJSONArray("siblings")
            val hashes = mutableMapOf<String, String>()

            for (i in 0 until siblings.length()) {
                val sibling = siblings.getJSONObject(i)
                val filename = sibling.getString("rfilename")
                val lfs = sibling.optJSONObject("lfs")
                if (lfs != null) {
                    val sha256 = lfs.optString("sha256", "")
                    if (sha256.isNotEmpty()) {
                        hashes[filename] = sha256
                        Log.d(TAG, "SHA-256 for $filename: ${sha256.take(16)}...")
                    }
                }
            }

            Log.d(TAG, "Fetched ${hashes.size} SHA-256 hashes from HuggingFace API")
            hashes
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching SHA-256 hashes", e)
            emptyMap()
        }
    }

    /**
     * Resolves manifest with actual SHA-256 hashes from HuggingFace.
     * Call this before starting download to enable DL-03 verification.
     */
    suspend fun resolveManifest() {
        val hashes = fetchSha256Hashes()
        if (hashes.isNotEmpty()) {
            resolvedManifest = DEFAULT_MANIFEST.copy(
                files = DEFAULT_MANIFEST.files.map { file ->
                    file.copy(expectedSha256 = hashes[file.relativePath] ?: file.expectedSha256)
                }
            )
            Log.d(TAG, "Manifest resolved with SHA-256 hashes for ${hashes.size} files")
        } else {
            Log.w(TAG, "Could not fetch SHA-256 hashes — verification will be skipped")
        }
    }

    /**
     * Checks if the device is on WiFi.
     * Per DL-06: Warn before downloading over cellular.
     */
    fun isOnWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Returns true if all model files are downloaded and verified.
     */
    suspend fun isModelReady(): Boolean {
        return storageManager.areAllModelFilesReady()
    }

    /**
     * Gets current download UI state.
     * Emits immediately then on changes.
     */
    fun observeDownloadState(): Flow<DownloadUiState> = flow {
        // Check if already ready
        if (storageManager.areAllModelFilesReady()) {
            emit(DownloadUiState.Ready)
            return@flow
        }

        // Check persisted state
        val states = downloadStateDao.getAllDownloadStates().first()
        val hasIncomplete = states.any { it.status != DownloadStateEntity.STATUS_COMPLETE }
        val hasAny = states.isNotEmpty()

        if (hasAny && !hasIncomplete) {
            emit(DownloadUiState.Complete)
        } else if (hasIncomplete) {
            emit(DownloadUiState.Paused)
        } else {
            emit(DownloadUiState.NotDownloaded)
        }
    }

    /**
     * Starts or resumes model download.
     * Per DL-01: Downloads from HuggingFace on first use.
     * Per DL-05: Progress emitted via return Flow.
     * Per DL-07: Resumes from persisted state after restart.
     *
     * @param skipWifiCheck If true, bypasses WiFi check (user confirmed cellular)
     * @return Flow of [DownloadUiState] for UI updates
     */
    fun downloadModel(skipWifiCheck: Boolean = false): Flow<DownloadUiState> = flow {
        downloadMutex.withLock {
            if (isDownloading.getAndSet(true)) {
                emit(DownloadUiState.Error("Download already in progress", canRetry = false))
                return@flow
            }

            try {
                // WiFi check per DL-06
                if (!skipWifiCheck && !isOnWifi()) {
                    val estimatedMB = DEFAULT_MANIFEST.files.sumOf { it.estimatedSizeBytes } / (1024.0 * 1024.0)
                    emit(DownloadUiState.WifiCheckRequired(estimatedMB))
                    isDownloading.set(false)
                    return@flow
                }

                // Resolve SHA-256 hashes per DL-03
                resolveManifest()

                val manifest = resolvedManifest
                val totalFiles = manifest.files.size

                // Count already-completed files from Room
                val completedStates = downloadStateDao.getAllDownloadStates().first()
                val completedPaths = completedStates
                    .filter { it.status == DownloadStateEntity.STATUS_COMPLETE }
                    .map { it.filePath }
                    .toSet()
                var completedFiles = completedPaths.size

                for (modelFile in manifest.files) {
                    val filePath = modelFile.relativePath

                    // Skip already completed
                    if (filePath in completedPaths) {
                        continue
                    }

                    // Get resume offset from persisted state
                    val existingState = downloadStateDao.getDownloadState(filePath)
                    val resumeFrom = existingState?.downloadedBytes ?: 0L

                    // Initialize or update state
                    downloadStateDao.insertDownloadState(
                        DownloadStateEntity(
                            filePath = filePath,
                            totalBytes = modelFile.estimatedSizeBytes,
                            downloadedBytes = resumeFrom,
                            expectedSha256 = modelFile.expectedSha256.ifBlank { null },
                            status = DownloadStateEntity.STATUS_DOWNLOADING
                        )
                    )

                    // Emit initial file-level progress
                    emit(
                        DownloadUiState.Downloading(
                            currentFile = filePath,
                            fileProgress = if (modelFile.estimatedSizeBytes > 0) {
                                (resumeFrom * 100 / modelFile.estimatedSizeBytes).toInt()
                            } else 0,
                            overallProgress = (completedFiles * 100) / totalFiles,
                            downloadedMB = resumeFrom / (1024.0 * 1024.0),
                            totalMB = modelFile.estimatedSizeBytes / (1024.0 * 1024.0),
                            speedMBPerSec = 0.0,
                            etaSeconds = 0
                        )
                    )

                    // Download with resume
                    val partFile = storageManager.getPartFilePath(filePath)
                    downloadService.downloadWithResume(
                        relativePath = filePath,
                        partFile = partFile,
                        startByte = resumeFrom,
                        expectedSha256 = modelFile.expectedSha256.ifBlank { null }
                    ).collect { progress ->
                        // Persist progress per DL-07
                        downloadStateDao.updateProgress(
                            filePath = filePath,
                            bytes = progress.downloadedBytes,
                            totalBytes = progress.totalBytes,
                            status = DownloadStateEntity.STATUS_DOWNLOADING
                        )

                        emit(
                            DownloadUiState.Downloading(
                                currentFile = filePath,
                                fileProgress = progress.percentage,
                                overallProgress = ((completedFiles * 100) + progress.percentage) / totalFiles,
                                downloadedMB = progress.downloadedMB,
                                totalMB = progress.totalMB,
                                speedMBPerSec = progress.speedMBPerSec,
                                etaSeconds = progress.etaSeconds
                            )
                        )
                    }

                    // Verify SHA-256 per DL-03
                    emit(DownloadUiState.Verifying(currentFile = filePath))
                    if (modelFile.expectedSha256.isNotBlank()) {
                        val isValid = downloadService.verifySha256(partFile, modelFile.expectedSha256)
                        if (!isValid) {
                            downloadStateDao.markFailed(filePath, "SHA-256 verification failed")
                            emit(DownloadUiState.Error("Verification failed for $filePath", canRetry = true))
                            return@flow
                        }
                    }

                    // Atomic rename per DL-04
                    storageManager.finalizePartFile(filePath)
                    downloadStateDao.markComplete(filePath, modelFile.expectedSha256)
                    completedFiles++
                }

                emit(DownloadUiState.Complete)
            } catch (e: Exception) {
                val message = e.message ?: "Download failed"
                Log.e(TAG, "Download failed", e)
                emit(DownloadUiState.Error(message, canRetry = true))
            } finally {
                isDownloading.set(false)
            }
        }
    }

    /**
     * Cancels ongoing download.
     */
    fun cancelDownload() {
        currentJob?.cancel()
        isDownloading.set(false)
    }

    /**
     * Deletes all model files and resets state.
     */
    suspend fun resetDownload() {
        cancelDownload()
        storageManager.deleteAllModelFiles()
        downloadStateDao.deleteAllDownloadStates()
    }

    /**
     * Gets total estimated size in MB.
     */
    fun getEstimatedSizeMB(): Double {
        return DEFAULT_MANIFEST.files.sumOf { it.estimatedSizeBytes } / (1024.0 * 1024.0)
    }
}
