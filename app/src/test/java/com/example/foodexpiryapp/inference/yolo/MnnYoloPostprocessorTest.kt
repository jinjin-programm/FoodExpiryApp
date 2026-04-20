package com.example.foodexpiryapp.inference.yolo

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.DetectionStatus
import com.example.foodexpiryapp.domain.model.FoodCategory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MnnYoloPostprocessorTest {

    // parseDetections tests

    @Test
    fun parseDetections_emptyArray_returnsEmptyList() {
        val result = MnnYoloPostprocessor.parseDetections(
            FloatArray(0), 0, 6, 640, 480
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun parseDetections_singleValidDetection_returnsOneResult() {
        val outputArray = floatArrayOf(
            100f, 100f, 200f, 200f, 0.95f, 0f  // x1, y1, x2, y2, confidence, class_id
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 6, 640, 480
        )
        assertEquals(1, result.size)
        assertEquals(0.95f, result[0].confidence, 0.01f)
    }

    @Test
    fun parseDetections_lowConfidence_filtered() {
        val outputArray = floatArrayOf(
            100f, 100f, 200f, 200f, 0.005f, 0f  // confidence below 0.01
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 6, 640, 480
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun parseDetections_degenerateBox_filtered() {
        val outputArray = floatArrayOf(
            200f, 200f, 100f, 100f, 0.95f, 0f  // x2 < x1, y2 < y1 - degenerate
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 6, 640, 480
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun parseDetections_multiClass_correctClassLabel() {
        val outputArray = floatArrayOf(
            100f, 100f, 200f, 200f, 0.95f, 0f,  // class scores start at index 5
            0.1f, 0.8f, 0.1f  // class_1 has highest score (0.8)
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 9, 640, 480
        )
        assertEquals(1, result.size)
        assertEquals("class_1", result[0].label)
    }

    @Test
    fun parseDetections_coordinatesNormalized_0_to_1() {
        val outputArray = floatArrayOf(
            0f, 0f, 640f, 640f, 0.95f, 0f  // full image in 640x640 space
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 6, 640, 480
        )
        assertEquals(1, result.size)
        // Coordinates should be normalized to 0f-1f range
        assertTrue(result[0].boundingBox.left >= 0f)
        assertTrue(result[0].boundingBox.top >= 0f)
        assertTrue(result[0].boundingBox.right <= 1f)
        assertTrue(result[0].boundingBox.bottom <= 1f)
    }

    @Test
    fun parseDetections_multipleDetections_allReturned() {
        val outputArray = floatArrayOf(
            10f, 10f, 100f, 100f, 0.9f, 0f,   // detection 1
            200f, 200f, 300f, 300f, 0.8f, 1f  // detection 2
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 2, 6, 640, 480
        )
        assertEquals(2, result.size)
        // Sorted by confidence descending
        assertTrue(result[0].confidence >= result[1].confidence)
    }

    // applyNms tests

    @Test
    fun applyNms_noOverlap_allKept() {
        val detections = listOf(
            DetectionResult(0, RectF(0f, 0f, 0.2f, 0.2f), null, "apple", FoodCategory.FRUITS, 0.9f),
            DetectionResult(1, RectF(0.6f, 0.6f, 0.8f, 0.8f), null, "banana", FoodCategory.FRUITS, 0.8f)
        )
        val result = MnnYoloPostprocessor.applyNms(detections, 0.5f)
        assertEquals(2, result.size)
    }

    @Test
    fun applyNms_highOverlap_lowerConfidenceRemoved() {
        val detections = listOf(
            DetectionResult(0, RectF(0f, 0f, 0.5f, 0.5f), null, "apple", FoodCategory.FRUITS, 0.9f),
            DetectionResult(1, RectF(0.1f, 0.1f, 0.4f, 0.4f), null, "banana", FoodCategory.FRUITS, 0.7f)
        )
        val result = MnnYoloPostprocessor.applyNms(detections, 0.3f)
        assertEquals(1, result.size)
        assertEquals(0.9f, result[0].confidence, 0.01f)
    }

    @Test
    fun applyNms_lowOverlap_bothKept() {
        val detections = listOf(
            DetectionResult(0, RectF(0f, 0f, 0.3f, 0.3f), null, "apple", FoodCategory.FRUITS, 0.9f),
            DetectionResult(1, RectF(0.5f, 0.5f, 0.8f, 0.8f), null, "banana", FoodCategory.FRUITS, 0.8f)
        )
        val result = MnnYoloPostprocessor.applyNms(detections, 0.3f)
        assertEquals(2, result.size)
    }

    @Test
    fun applyNms_emptyInput_emptyOutput() {
        val result = MnnYoloPostprocessor.applyNms(emptyList(), 0.5f)
        assertTrue(result.isEmpty())
    }

    // cropDetection tests

    @Test
    fun cropDetection_validCrop_returnsBitmap() {
        val testBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val detection = DetectionResult(
            id = 0,
            boundingBox = RectF(0.1f, 0.2f, 0.5f, 0.8f),
            label = "apple",
            category = FoodCategory.FRUITS,
            confidence = 0.95f,
            status = DetectionStatus.PENDING
        )
        val crop = MnnYoloPostprocessor.cropDetection(testBitmap, detection)
        assertNotNull(crop)
        assertTrue(crop!!.width > 0)
        assertTrue(crop.height > 0)
        crop.recycle()
        testBitmap.recycle()
    }

    @Test
    fun cropDetection_outOfBounds_returnsNull() {
        val testBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val detection = DetectionResult(
            id = 0,
            boundingBox = RectF(-0.1f, 0.2f, 0.5f, 0.8f),  // left < 0
            label = "apple",
            category = FoodCategory.FRUITS,
            confidence = 0.95f,
            status = DetectionStatus.PENDING
        )
        val crop = MnnYoloPostprocessor.cropDetection(testBitmap, detection)
        assertNull(crop)
        testBitmap.recycle()
    }

    @Test
    fun cropDetection_degenerateBox_returnsNull() {
        val testBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        val detection = DetectionResult(
            id = 0,
            boundingBox = RectF(0.3f, 0.3f, 0.3f, 0.5f),  // right <= left
            label = "apple",
            category = FoodCategory.FRUITS,
            confidence = 0.95f,
            status = DetectionStatus.PENDING
        )
        val crop = MnnYoloPostprocessor.cropDetection(testBitmap, detection)
        assertNull(crop)
        testBitmap.recycle()
    }

    // mapLabelToCategory tests (indirect via parseDetections)

    @Test
    fun mapLabelToCategory_knownFood_correctCategory() {
        val outputArray = floatArrayOf(
            100f, 100f, 200f, 200f, 0.95f, 0f,  // class_0 = apple
            0.9f, 0f, 0f,  // class 0 has high score
            0.1f           // class 1
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 10, 640, 480
        )
        assertEquals(1, result.size)
        assertEquals(FoodCategory.FRUITS, result[0].category)
    }

    @Test
    fun mapLabelToCategory_unknownLabel_returnsOther() {
        val outputArray = floatArrayOf(
            100f, 100f, 200f, 200f, 0.95f, 0f,
            0.1f, 0.1f, 0.1f, 0.1f, 0.1f  // all class scores low
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 11, 640, 480
        )
        assertEquals(1, result.size)
        assertEquals(FoodCategory.OTHER, result[0].category)
    }

    @Test
    fun mapLabelToCategory_meatCategory() {
        val outputArray = floatArrayOf(
            100f, 100f, 200f, 200f, 0.95f, 0f,
            0.9f, 0f,  // class 1 has high score
            0.1f, 0.1f, 0.1f, 0.1f
        )
        val result = MnnYoloPostprocessor.parseDetections(
            outputArray, 1, 10, 640, 480
        )
        assertEquals(1, result.size)
        // class_1 doesn't match any known food, should be OTHER
        assertEquals(FoodCategory.OTHER, result[0].category)
    }
}
