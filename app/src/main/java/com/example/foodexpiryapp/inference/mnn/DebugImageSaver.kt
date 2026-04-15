package com.example.foodexpiryapp.inference.mnn

import java.io.File

class DebugImageSaver(private val rootDir: File) {

    fun saveJpeg(bytes: ByteArray, baseName: String): File {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        val file = File(rootDir, "$baseName.jpg")
        file.writeBytes(bytes)
        return file
    }
}
