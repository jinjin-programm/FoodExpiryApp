package com.example.foodexpiryapp.inference.yolo

import org.junit.Assert.*
import org.junit.Test

class YoloEPostprocessorTest {

    private fun createYoloEOutput(
        numDetections: Int,
        numValues: Int = 38,
        configDetections: List<Triple<FloatArray, Float, Int>>
    ): FloatArray {
        val output = FloatArray(numDetections * numValues)
        for ((i, triple) in configDetections.withIndex()) {
            val (bbox, score, classId) = triple
            val base = i * numValues
            System.arraycopy(bbox, 0, output, base, 4)
            output[base + numValues - 2] = score
            output[base + numValues - 1] = classId.toFloat()
        }
        return output
    }

    @Test
    fun testParseYoloE_validDetections() {
        val output = createYoloEOutput(3, 38, listOf(
            Triple(floatArrayOf(100f, 100f, 300f, 300f), 0.9f, 0),
            Triple(floatArrayOf(200f, 200f, 400f, 400f), 0.8f, 1),
            Triple(floatArrayOf(50f, 50f, 150f, 150f), 0.7f, 2)
        ))

        val results = MnnYoloPostprocessor.parseYoloEDetections(
            output, 3, 38, 1920, 1080, 0.5f, 8
        )

        assertEquals(3, results.size)
        assertEquals(0.9f, results[0].confidence, 0.01f)
        assertEquals("class_0", results[0].label)
        assertEquals(0.8f, results[1].confidence, 0.01f)
        assertEquals("class_1", results[1].label)
        assertEquals(0.7f, results[2].confidence, 0.01f)
        assertEquals("class_2", results[2].label)
    }

    @Test
    fun testParseYoloE_filtersLowConfidence() {
        val output = createYoloEOutput(5, 38, listOf(
            Triple(floatArrayOf(100f, 100f, 300f, 300f), 0.7f, 0),
            Triple(floatArrayOf(200f, 200f, 400f, 400f), 0.8f, 1),
            Triple(floatArrayOf(50f, 50f, 150f, 150f), 0.6f, 2),
            Triple(floatArrayOf(10f, 10f, 50f, 50f), 0.2f, 3),
            Triple(floatArrayOf(20f, 20f, 60f, 60f), 0.3f, 4)
        ))

        val results = MnnYoloPostprocessor.parseYoloEDetections(
            output, 5, 38, 640, 640, 0.5f, 8
        )

        assertEquals(3, results.size)
        assertTrue(results.all { it.confidence >= 0.5f })
    }

    @Test
    fun testParseYoloE_maxDetections() {
        val detections = (0 until 10).map { i ->
            Triple(floatArrayOf(10f * i, 10f * i, 100f + 10f * i, 100f + 10f * i), 0.6f + i * 0.03f, i)
        }
        val output = createYoloEOutput(10, 38, detections)

        val results = MnnYoloPostprocessor.parseYoloEDetections(
            output, 10, 38, 640, 640, 0.5f, 8
        )

        assertEquals(8, results.size)
        assertTrue(results[0].confidence >= results[1].confidence)
    }

    @Test
    fun testParseYoloE_emptyOutput() {
        val results = MnnYoloPostprocessor.parseYoloEDetections(
            FloatArray(0), 0, 38, 640, 640, 0.5f, 8
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun testParseYoloE_degenerateBoxes() {
        val output = createYoloEOutput(3, 38, listOf(
            Triple(floatArrayOf(300f, 300f, 100f, 100f), 0.9f, 0),
            Triple(floatArrayOf(200f, 200f, 200f, 400f), 0.8f, 1),
            Triple(floatArrayOf(50f, 50f, 150f, 50f), 0.7f, 2)
        ))

        val results = MnnYoloPostprocessor.parseYoloEDetections(
            output, 3, 38, 640, 640, 0.5f, 8
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun testParseYoloE_normalizedCoordinates() {
        val output = createYoloEOutput(1, 38, listOf(
            Triple(floatArrayOf(0f, 0f, 640f, 640f), 0.9f, 0)
        ))

        val results = MnnYoloPostprocessor.parseYoloEDetections(
            output, 1, 38, 640, 640, 0.5f, 8
        )

        assertEquals(1, results.size)
        assertTrue(results[0].boundingBox.left >= 0f)
        assertTrue(results[0].boundingBox.top >= 0f)
        assertTrue(results[0].boundingBox.right <= 1f)
        assertTrue(results[0].boundingBox.bottom <= 1f)
    }

    @Test
    fun testApplyNms_returnsFewerOrEqual() {
        val detections = (0 until 5).map { i ->
            createDetection(0.9f - i * 0.1f, 0.1f * i, 0.1f * i, 0.1f * i + 0.2f, 0.1f * i + 0.2f)
        }

        val results = MnnYoloPostprocessor.applyNms(detections, 0.45f)

        assertTrue(results.size <= detections.size)
    }

    @Test
    fun testApplyNms_keepsNonOverlapping() {
        val d1 = createDetection(0.9f, 0.1f, 0.1f, 0.3f, 0.3f)
        val d2 = createDetection(0.8f, 0.7f, 0.7f, 0.9f, 0.9f)

        val results = MnnYoloPostprocessor.applyNms(listOf(d1, d2), 0.5f)

        assertEquals(2, results.size)
    }

    @Test
    fun testCropDetection_validCropSkippedInJvm() {
        val bitmap: android.graphics.Bitmap? = try {
            android.graphics.Bitmap.createBitmap(640, 480, android.graphics.Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
        if (bitmap == null) return

        val detection = createDetection(0.9f, 0.1f, 0.2f, 0.5f, 0.8f)
        val cropped = MnnYoloPostprocessor.cropDetection(bitmap, detection)
        assertNotNull(cropped)
        assertTrue(cropped!!.width > 0)
        assertTrue(cropped.height > 0)
    }

    @Test
    fun testCropDetection_outOfBoundsReturnsNullSkippedInJvm() {
        val bitmap: android.graphics.Bitmap? = try {
            android.graphics.Bitmap.createBitmap(640, 480, android.graphics.Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
        if (bitmap == null) return

        val detection = createDetection(0.9f, 0.9f, 0.9f, 1.5f, 1.5f)
        val cropped = MnnYoloPostprocessor.cropDetection(bitmap, detection)
        assertNull(cropped)
    }

    private fun createDetection(
        confidence: Float,
        left: Float, top: Float, right: Float, bottom: Float
    ): com.example.foodexpiryapp.domain.model.DetectionResult {
        return com.example.foodexpiryapp.domain.model.DetectionResult(
            id = 0,
            boundingBox = android.graphics.RectF(left, top, right, bottom),
            label = "test",
            category = com.example.foodexpiryapp.domain.model.FoodCategory.OTHER,
            confidence = confidence
        )
    }
}
