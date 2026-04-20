package com.example.foodexpiryapp.util

import android.content.Context
import android.graphics.Bitmap
import com.example.foodexpiryapp.util.AppLog
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodImageStorage @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FoodImageStorage"
        private const val DIR_NAME = "food_images"
        private const val MAX_SIZE = 512
        private const val JPEG_QUALITY = 85
    }

    private val imageDir: File
        get() = File(context.filesDir, DIR_NAME).also { if (!it.exists()) it.mkdirs() }

    fun saveFromPath(sourcePath: String, foodItemId: Long): String? {
        return try {
            val source = File(sourcePath)
            if (!source.exists()) return null
            val dest = File(imageDir, "${foodItemId}.jpg")
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            dest.absolutePath
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to copy food image from $sourcePath", e)
            null
        }
    }

    fun saveFromBitmap(bitmap: Bitmap, foodItemId: Long): String? {
        return try {
            val resized = resizeIfNeeded(bitmap)
            val dest = File(imageDir, "${foodItemId}.jpg")
            FileOutputStream(dest).use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (resized !== bitmap) resized.recycle()
            dest.absolutePath
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to save food image bitmap for item $foodItemId", e)
            null
        }
    }

    fun deleteImage(imagePath: String?) {
        if (imagePath.isNullOrBlank()) return
        try {
            val file = File(imagePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to delete food image: $imagePath", e)
        }
    }

    fun deleteAllImages() {
        try {
            val dir = imageDir
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to delete all food images", e)
        }
    }

    private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_SIZE && bitmap.height <= MAX_SIZE) return bitmap
        val scale = MAX_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val newW = (bitmap.width * scale).toInt()
        val newH = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
