package com.example.foodexpiryapp.presentation.util

import android.content.Context
import android.net.Uri
import java.io.File

object PhotoStorageHelper {
    private const val CACHE_DIR_NAME = "profile_photos"

    /**
     * Copies a photo from the given URI to the app's cache directory.
     * Returns the path to the cached file.
     */
    fun savePhotoCached(context: Context, sourceUri: Uri): String? {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val fileName = "profile_photo_${System.currentTimeMillis()}.jpg"
            val cachedFile = File(cacheDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                cachedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            cachedFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Gets the cached profile photo file, if it exists.
     */
    fun getCachedPhotoFile(context: Context, fileName: String): File? {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        val file = File(cacheDir, fileName)
        return if (file.exists()) file else null
    }

    /**
     * Gets the path to the profile photos cache directory.
     */
    fun getCacheDir(context: Context): File {
        return File(context.cacheDir, CACHE_DIR_NAME)
    }
}
