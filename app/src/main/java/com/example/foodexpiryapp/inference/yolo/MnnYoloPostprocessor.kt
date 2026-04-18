package com.example.foodexpiryapp.inference.yolo

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.DetectionStatus
import com.example.foodexpiryapp.domain.model.FoodCategory
import kotlin.math.max
import kotlin.math.min

/**
 * Postprocessor for YOLO model output.
 *
 * Handles:
 * 1. Parsing raw YOLO output array into domain DetectionResult objects
 * 2. Coordinate normalization from 640x640 space to 0f-1f relative to original image
 * 3. Non-maximum suppression (NMS) for overlapping detections
 * 4. Bitmap cropping for detected regions
 *
 * Per YOLO-07: Results are capped at the configured maxDetections limit.
 */
object MnnYoloPostprocessor {

    private const val INPUT_SIZE = 640

    /**
     * Parses raw YOLO output array into domain DetectionResult list.
     *
     * Expected output format (per row):
     *   [x1, y1, x2, y2, confidence, class_id, ...class_scores]
     *   Coordinates in 640x640 pixel space.
     *
     * Output coordinates are normalized to 0f-1f relative to original image dimensions,
     * accounting for the letterbox resize (padding) applied during preprocessing.
     *
     * @param outputArray Raw float array from YOLO model output
     * @param numDetections Number of detection rows in output
     * @param numValues Number of values per detection row
     * @param originalWidth Original image width in pixels
     * @param originalHeight Original image height in pixels
     * @return List of DetectionResult with normalized bounding box coordinates
     */
    fun parseDetections(
        outputArray: FloatArray,
        numDetections: Int,
        numValues: Int,
        originalWidth: Int,
        originalHeight: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()

        for (i in 0 until numDetections) {
            val base = i * numValues
            if (base + 5 > outputArray.size) break

            val confidence = outputArray[base + 4]
            if (confidence < 0.01f) continue  // Skip very low confidence

            // Parse bounding box in 640x640 space
            val x1 = outputArray[base].coerceIn(0f, 640f)
            val y1 = outputArray[base + 1].coerceIn(0f, 640f)
            val x2 = outputArray[base + 2].coerceIn(0f, 640f)
            val y2 = outputArray[base + 3].coerceIn(0f, 640f)

            // Skip degenerate boxes
            if (x2 <= x1 || y2 <= y1) continue

            // Determine class label
            val label = if (numValues > 6) {
                // Multi-class: find argmax of class scores starting at index 5
                val classStart = base + 5
                val classEnd = min(classStart + (numValues - 5), outputArray.size)
                if (classEnd > classStart) {
                    var maxScore = -1f
                    var maxIdx = 0
                    for (j in classStart until classEnd) {
                        if (outputArray[j] > maxScore) {
                            maxScore = outputArray[j]
                            maxIdx = j - classStart
                        }
                    }
                    "class_$maxIdx"
                } else {
                    "food"
                }
            } else {
                // 6-value format: class_id at index 5
                if (base + 5 < outputArray.size) {
                    val classId = outputArray[base + 5].toInt()
                    "class_$classId"
                } else {
                    "food"
                }
            }

            // Normalize coordinates from 640x640 space to 0f-1f relative to original image
            // For simplicity, assuming letterbox resize where the longer side maps to 640
            val scale = 640f / max(originalWidth, originalHeight)
            val offsetX = (640f - originalWidth * scale) / 2f
            val offsetY = (640f - originalHeight * scale) / 2f

            val normX1 = ((x1 - offsetX) / scale / originalWidth).coerceIn(0f, 1f)
            val normY1 = ((y1 - offsetY) / scale / originalHeight).coerceIn(0f, 1f)
            val normX2 = ((x2 - offsetX) / scale / originalWidth).coerceIn(0f, 1f)
            val normY2 = ((y2 - offsetY) / scale / originalHeight).coerceIn(0f, 1f)

            val category = mapLabelToCategory(label)

            results.add(
                DetectionResult(
                    id = i,
                    boundingBox = RectF(normX1, normY1, normX2, normY2),
                    label = label,
                    category = category,
                    confidence = confidence,
                    status = DetectionStatus.PENDING
                )
            )
        }

        return results
    }

    fun parseYoloEDetections(
        outputArray: FloatArray,
        numDetections: Int,
        numValues: Int,
        originalWidth: Int,
        originalHeight: Int,
        confidenceThreshold: Float = 0.5f,
        maxDetections: Int = 8,
        letterboxScale: Float = 0f,
        letterboxPadW: Float = 0f,
        letterboxPadH: Float = 0f,
        numMasks: Int = 0
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val numClasses = numValues - 4 - numMasks

        val useLetterbox = letterboxScale > 0f

        for (i in 0 until numDetections) {
            val base = i * numValues
            if (base + numValues > outputArray.size) break

            var x1 = outputArray[base]
            var y1 = outputArray[base + 1]
            var x2 = outputArray[base + 2]
            var y2 = outputArray[base + 3]

            if (maxOf(x1, y1, x2, y2) <= 2.0f) {
                x1 *= INPUT_SIZE; y1 *= INPUT_SIZE; x2 *= INPUT_SIZE; y2 *= INPUT_SIZE
            }

            x1 = x1.coerceIn(0f, INPUT_SIZE.toFloat())
            y1 = y1.coerceIn(0f, INPUT_SIZE.toFloat())
            x2 = x2.coerceIn(0f, INPUT_SIZE.toFloat())
            y2 = y2.coerceIn(0f, INPUT_SIZE.toFloat())

            if (x2 <= x1 || y2 <= y1) continue

            var maxClassScore = -1f
            var maxClassIdx = 0
            for (c in 0 until numClasses) {
                val score = outputArray[base + 4 + c]
                if (score > maxClassScore) {
                    maxClassScore = score
                    maxClassIdx = c
                }
            }

            val confidence = maxClassScore
            if (confidence < confidenceThreshold) continue

            val classId = maxClassIdx
            val label = "class_$classId"

            val normX1: Float
            val normY1: Float
            val normX2: Float
            val normY2: Float

            if (useLetterbox) {
                val realX1 = (x1 - letterboxPadW) / letterboxScale
                val realY1 = (y1 - letterboxPadH) / letterboxScale
                val realX2 = (x2 - letterboxPadW) / letterboxScale
                val realY2 = (y2 - letterboxPadH) / letterboxScale
                normX1 = (realX1 / originalWidth).coerceIn(0f, 1f)
                normY1 = (realY1 / originalHeight).coerceIn(0f, 1f)
                normX2 = (realX2 / originalWidth).coerceIn(0f, 1f)
                normY2 = (realY2 / originalHeight).coerceIn(0f, 1f)
            } else {
                val scale = INPUT_SIZE.toFloat() / max(originalWidth, originalHeight)
                val offsetX = (INPUT_SIZE.toFloat() - originalWidth * scale) / 2f
                val offsetY = (INPUT_SIZE.toFloat() - originalHeight * scale) / 2f
                normX1 = ((x1 - offsetX) / scale / originalWidth).coerceIn(0f, 1f)
                normY1 = ((y1 - offsetY) / scale / originalHeight).coerceIn(0f, 1f)
                normX2 = ((x2 - offsetX) / scale / originalWidth).coerceIn(0f, 1f)
                normY2 = ((y2 - offsetY) / scale / originalHeight).coerceIn(0f, 1f)
            }

            if (normX2 - normX1 < 0.005f || normY2 - normY1 < 0.005f) continue

            val category = mapLabelToCategory(label)

            results.add(
                DetectionResult(
                    id = i,
                    boundingBox = RectF(normX1, normY1, normX2, normY2),
                    label = label,
                    category = category,
                    confidence = confidence,
                    status = DetectionStatus.PENDING
                )
            )
        }

        return results.sortedByDescending { it.confidence }.take(maxDetections)
    }

    /**
     * Applies Non-Maximum Suppression to remove overlapping detections.
     *
     * Uses greedy NMS: sort by confidence descending, keep highest, remove
     * overlapping boxes above IoU threshold, repeat.
     *
     * @param detections List of DetectionResult to filter
     * @param iouThreshold IoU threshold above which boxes are considered overlapping
     * @return Filtered list with overlapping detections removed
     */
    fun applyNms(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<DetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            sorted.removeAll { other ->
                iou(best.boundingBox, other.boundingBox) > iouThreshold
            }
        }

        return kept
    }

    /**
     * Crops the bounding box region from the source bitmap.
     *
     * Uses normalized coordinates (0f-1f) from the DetectionResult to compute
     * pixel coordinates relative to the source bitmap dimensions.
     *
     * Per T-06-04: Validates coordinates to prevent out-of-bounds crop.
     *
     * @param sourceBitmap Original full-size bitmap to crop from
     * @param detection DetectionResult with normalized bounding box
     * @return Cropped bitmap, or null if coordinates are invalid
     */
    fun cropDetection(sourceBitmap: Bitmap, detection: DetectionResult): Bitmap? {
        val box = detection.boundingBox

        // Validate normalized coordinates
        if (box.left < 0f || box.top < 0f || box.right > 1f || box.bottom > 1f) return null
        if (box.right <= box.left || box.bottom <= box.top) return null

        val x = (box.left * sourceBitmap.width).toInt().coerceIn(0, sourceBitmap.width - 1)
        val y = (box.top * sourceBitmap.height).toInt().coerceIn(0, sourceBitmap.height - 1)
        val w = ((box.right - box.left) * sourceBitmap.width).toInt().coerceIn(1, sourceBitmap.width - x)
        val h = ((box.bottom - box.top) * sourceBitmap.height).toInt().coerceIn(1, sourceBitmap.height - y)

        if (w <= 0 || h <= 0) return null

        return try {
            Bitmap.createBitmap(sourceBitmap, x, y, w, h)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes Intersection over Union for two bounding boxes.
     */
    private fun iou(a: RectF, b: RectF): Float {
        val ix1 = max(a.left, b.left)
        val iy1 = max(a.top, b.top)
        val ix2 = min(a.right, b.right)
        val iy2 = min(a.bottom, b.bottom)
        if (ix2 <= ix1 || iy2 <= iy1) return 0f
        val inter = (ix2 - ix1) * (iy2 - iy1)
        val aArea = (a.right - a.left) * (a.bottom - a.top)
        val bArea = (b.right - b.left) * (b.bottom - b.top)
        val union = aArea + bArea - inter
        return if (union > 0f) inter / union else 0f
    }

    /**
     * Maps a YOLO class label to a FoodCategory.
     * Uses the same mapping as the existing YoloDetector for consistency.
     */
    private fun mapLabelToCategory(label: String): FoodCategory {
        val key = label.lowercase()
        return when {
            // FRUITS
            key in listOf("apple", "banana", "grape", "lemon", "orange", "pineapple", "watermelon")
                -> FoodCategory.FRUITS
            // VEGETABLES
            key in listOf("beans", "chilli", "corn", "tomato", "broccoli", "carrot", "canned_corn", "canned_soup", "canned_tomatoes")
                -> FoodCategory.VEGETABLES
            // MEAT
            key in listOf("fish", "beef_meat", "chicken_meat", "pork_meat", "lamb_meat", "duck_meat", "turkey_meat",
                "crab_meat", "shrimp", "scallops", "tofu", "cod_fish", "mackerel_fish", "salmon_fish", "tuna_fish",
                "sardines", "canned_tuna", "hot dog")
                -> FoodCategory.MEAT
            // DAIRY
            key in listOf("milk", "eggs", "butter", "cheese", "cream", "yogurt")
                -> FoodCategory.DAIRY
            // GRAINS
            key in listOf("cereal", "flour", "pasta", "rice", "bagel", "baguette", "croissant", "noodles",
                "oatmeal", "tortilla", "wheat_flour", "white_bread", "canned_beans", "sandwich", "pizza")
                -> FoodCategory.GRAINS
            // CONDIMENTS
            key in listOf("honey", "jam", "oil", "spices", "sugar", "tomato_sauce", "vinegar",
                "apple_cider_vinegar", "bbq_sauce", "coconut_oil", "curry_paste", "fish_sauce", "ketchup",
                "mayonnaise", "mustard", "olive_oil", "oyster_sauce", "rice_vinegar", "salt", "soy_sauce",
                "white_sugar", "black_pepper")
                -> FoodCategory.CONDIMENTS
            // SNACKS
            key in listOf("cake", "candy", "chips", "chocolate", "nuts", "donut")
                -> FoodCategory.SNACKS
            // BEVERAGES
            key in listOf("coffee", "juice", "soda", "tea", "water", "coffee_beans", "tea_bags")
                -> FoodCategory.BEVERAGES
            else -> FoodCategory.OTHER
        }
    }
}
