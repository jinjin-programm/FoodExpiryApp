package com.example.foodexpiryapp.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages model file storage on the device filesystem.
 *
 * Per PITFALL-4: Uses externalFilesDir to avoid internal storage quota limits.
 * Per DL-04: Downloads go to .part files, renamed atomically after SHA-256 verification.
 * Per PITFALL-3: Never load model file while .part exists; cleanup incomplete files on startup.
 */
@Singleton
class ModelStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val MODEL_DIR_NAME = "mnn_models"
        const val PART_EXTENSION = ".part"

        /** Required model files for the Qwen3.5-2B-MNN model */
        val REQUIRED_MODEL_FILES = listOf(
            "llm.mnn",
            "llm.mnn.weight",
            "llm_config.json",
            "config.json",
            "tokenizer.txt",
            "visual.mnn"
        )
    }

    /**
     * Gets the root model storage directory.
     * Per PITFALL-4: Uses external files dir to avoid internal storage quota.
     */
    fun getModelDirectory(): File {
        val dir = File(context.getExternalFilesDir(null), MODEL_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Gets the path for a .part file during download.
     * Per DL-04: Partial downloads use .part file pattern.
     *
     * @param relativePath Path within the model directory (e.g., "llm.mnn.weight")
     * @return File with .part extension (e.g., "llm.mnn.weight.part")
     */
    fun getPartFilePath(relativePath: String): File {
        return File(getModelDirectory(), "$relativePath$PART_EXTENSION")
    }

    /**
     * Gets the path for the final model file after verification.
     *
     * @param relativePath Path within the model directory (e.g., "llm.mnn.weight")
     * @return Final file path (e.g., "llm.mnn.weight")
     */
    fun getFinalFilePath(relativePath: String): File {
        return File(getModelDirectory(), relativePath)
    }

    /**
     * Atomically renames .part file to the final model file after SHA-256 verification.
     * Per DL-04: Atomic rename after verification.
     * Per PITFALL-3: Prevents mmap deadlock on partial files — only final files should be loaded.
     *
     * @param relativePath Path within the model directory
     * @return true if the rename succeeded, false if the .part file doesn't exist
     */
    fun finalizePartFile(relativePath: String): Boolean {
        val partFile = getPartFilePath(relativePath)
        val finalFile = getFinalFilePath(relativePath)

        if (!partFile.exists()) return false

        // Delete existing final file if present (re-download case)
        if (finalFile.exists()) finalFile.delete()

        return partFile.renameTo(finalFile)
    }

    /**
     * Checks if all required model files exist and are complete (no .part files present).
     * Per PITFALL-3: A .part file means the download is incomplete — model should not be loaded.
     *
     * @return true if every required model file exists and no .part files remain
     */
    fun areAllModelFilesReady(): Boolean {
        return REQUIRED_MODEL_FILES.all { relativePath ->
            val finalFile = getFinalFilePath(relativePath)
            val partFile = getPartFilePath(relativePath)
            finalFile.exists() && !partFile.exists()
        }
    }

    /**
     * Gets total size of all finalized model files (excludes .part files).
     *
     * @return Total size in bytes
     */
    fun getTotalModelSize(): Long {
        return getModelDirectory().listFiles()
            ?.filter { !it.name.endsWith(PART_EXTENSION) && it.isFile }
            ?.sumOf { it.length() }
            ?: 0L
    }

    /**
     * Gets total size of all .part files (incomplete downloads).
     *
     * @return Total size of .part files in bytes
     */
    fun getTotalPartFileSize(): Long {
        return getModelDirectory().listFiles()
            ?.filter { it.name.endsWith(PART_EXTENSION) }
            ?.sumOf { it.length() }
            ?: 0L
    }

    /**
     * Gets the size of a specific .part file.
     * Useful for determining the resume byte offset.
     *
     * @param relativePath Path within the model directory
     * @return Size in bytes, or 0 if the .part file doesn't exist
     */
    fun getPartFileSize(relativePath: String): Long {
        val partFile = getPartFilePath(relativePath)
        return if (partFile.exists()) partFile.length() else 0L
    }

    /**
     * Cleans up incomplete .part files from interrupted downloads.
     * Per PITFALL-3: Delete incomplete files on startup to prevent stale state.
     *
     * @return Number of files cleaned up
     */
    fun cleanupIncompleteDownloads(): Int {
        val modelDir = getModelDirectory()
        val partFiles = modelDir.listFiles()
            ?.filter { it.name.endsWith(PART_EXTENSION) }
            ?: emptyList()

        partFiles.forEach { it.delete() }
        return partFiles.size
    }

    /**
     * Deletes all model files (both final and .part files).
     * Used for full re-download or uninstall of the model.
     *
     * @return true if all files were deleted successfully
     */
    fun deleteAllModelFiles(): Boolean {
        val modelDir = getModelDirectory()
        val files = modelDir.listFiles() ?: return true
        var allDeleted = true
        files.forEach { file ->
            if (!file.delete()) allDeleted = false
        }
        return allDeleted
    }

    /**
     * Deletes a specific model file and its .part file.
     *
     * @param relativePath Path within the model directory
     */
    fun deleteModelFile(relativePath: String) {
        getFinalFilePath(relativePath).delete()
        getPartFilePath(relativePath).delete()
    }
}
