package com.example.foodexpiryapp.inference.mnn

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DebugImageSaverTest {

    @Test
    fun saveJpeg_writesBytesToDisk() {
        val tempDir = Files.createTempDirectory("debug-image-saver").toFile()
        val saver = DebugImageSaver(tempDir)
        val payload = byteArrayOf(1, 2, 3, 4, 5)

        val savedFile = saver.saveJpeg(payload, "vision_input")

        assertEquals(tempDir, savedFile.parentFile)
        assertEquals("vision_input.jpg", savedFile.name)
        assertArrayEquals(payload, savedFile.readBytes())
    }
}
