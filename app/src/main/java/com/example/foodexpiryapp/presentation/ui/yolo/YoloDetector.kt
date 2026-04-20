package com.example.foodexpiryapp.presentation.ui.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import com.example.foodexpiryapp.util.AppLog
import com.example.foodexpiryapp.domain.model.FoodCategory
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.math.min

/**
 * YOLO26n object detector.
 *
 * Supports the Ultralytics YOLO26n TFLite export (NMS-free / end-to-end mode).
 *
 * Model export (Python):
 *   from ultralytics import YOLO
 *   YOLO("yolo26n.pt").export(format="tflite")
 *   # → yolo26n_float32.tflite
 *
 * Model I/O:
 *   Input  : [1, 640, 640, 3]  float32, normalised to [0, 1]
 *   Output : [1, 300, 6]       float32  (NMS-free end-to-end head)
 *            each row → [x1, y1, x2, y2, confidence, class_id]
 *            coordinates are in absolute pixels relative to the 640×640 input space.
 *
 * Fallback: if yolo26n_float32.tflite is absent the detector tries the older
 * food-specific models that were already present in the project.
 */
class YoloDetector(
    context: Context,
    private val modelName: String = DEFAULT_MODEL_NAME
) {

    companion object {
        private const val TAG = "YoloDetector"

        // YOLO26n custom food model (34 classes, NMS-free TFLite export)
        const val DEFAULT_MODEL_NAME = "food_yolo26n_float32.tflite"

        // Fallback models tried in order when the primary is unavailable
        // crawled_grocery_yolo11n supports 51 classes including eggs, butter, cheese, etc.
        private const val FALLBACK_MODELS =
            "crawled_grocery_yolo11n.tflite,yolo26n_float32.tflite,foodvision_yolov8.tflite,yolo11n_float32.tflite"

        // Labels files
        private const val COCO_LABELS_FILE = "coco_labels.txt"
        private const val FOOD_CATEGORIES_FILE = "food_categories.txt"
        private const val LEGACY_FOOD_LABELS_FILE = "yolo_labels.txt"
        private const val CRAWLED_GROCERY_LABELS_FILE = "yolo_labels_crawled_grocery.txt"

        private const val INPUT_SIZE = 640

        // YOLO26n NMS-free export: up to 300 detections
        private const val MAX_DETECTIONS = 300

        // Minimum score to keep a detection — kept low so the overlay draws
        // the red box early; the UI applies its own 40 % display threshold.
        private const val CONFIDENCE_THRESHOLD = 0.25f

        // IoU threshold for the optional safety-net NMS applied to fallback models
        private const val IOU_THRESHOLD = 0.45f
    }

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    private val foodCategoryMap = mutableMapOf<String, FoodCategory>()

    /** True when using an NMS-free YOLO26n model (vs. a legacy fallback). */
    private var usingNmsFreeModel = false

    /** The actual model file that was loaded. */
    private var loadedModelFile = ""

    init {
        try {
            loadModel(context)
            loadLabels(context)
            buildFoodCategoryMap()
        } catch (e: Exception) {
            AppLog.e(TAG, "Error initialising YoloDetector", e)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Initialisation helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun loadModel(context: Context) {
        // Try the primary model first, then each fallback in order
        val candidates = listOf(modelName) + FALLBACK_MODELS.split(",").map { it.trim() }

        for (candidate in candidates) {
            try {
                val buffer = FileUtil.loadMappedFile(context, candidate)
                val options = Interpreter.Options().apply { numThreads = 4 }
                interpreter = Interpreter(buffer, options)
                
        // NMS-free head is used for yolo26n exports only; yolo11n uses classic head
                usingNmsFreeModel = candidate.contains("yolo26n")
                loadedModelFile = candidate
                AppLog.i(TAG, "Loaded model: \$candidate (nmsFree=\$usingNmsFreeModel)")

                // Log actual output tensor shape for debugging
                interpreter?.let { interp ->
                    val outShape = interp.getOutputTensor(0).shape()
                    AppLog.i(TAG, "Output tensor shape: ${outShape.toList()}")
                }
                return
            } catch (e: IOException) {
                AppLog.w(TAG, "Model not found or failed to load: $candidate")
            }
        }
        AppLog.e(TAG, "No YOLO model could be loaded. Tried: ${candidates.joinToString()}")
    }

    private fun loadLabels(context: Context) {
        val interp = interpreter ?: return
        val outShape = interp.getOutputTensor(0).shape()
        // outShape = [batch, detections, 4 + 1 + num_classes] or [batch, 300, 6]
        // In the NMS-free case [1, 300, 6], values are [x, y, x, y, conf, class_id]
        // But for training with 11 classes, the model still uses the same head structure.
        
        // Decide which label file to use based on the loaded model filename.
        // - "food_yolo26n"         → custom 34-class food model
        // - "crawled_grocery"      → 51-class grocery model (includes eggs)
        // - "yolo26n"              → original 80-class COCO pretrained
        // - others                 → legacy yolo_labels.txt
        val file = when {
            loadedModelFile.contains("food_yolo26n")    -> FOOD_CATEGORIES_FILE
            loadedModelFile.contains("crawled_grocery") -> CRAWLED_GROCERY_LABELS_FILE
            loadedModelFile.contains("yolo26n")         -> COCO_LABELS_FILE
            else                                        -> LEGACY_FOOD_LABELS_FILE
        }

        try {
            context.assets.open(file).use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { lines ->
                    lines.forEach { 
                        val label = it.trim()
                        if (label.isNotEmpty()) labels.add(label) 
                    }
                }
            }
            AppLog.i(TAG, "Loaded ${labels.size} labels from $file")
        } catch (e: IOException) {
            AppLog.e(TAG, "Could not load labels file $file: ${e.message}")
            labels.addAll(listOf("DAIRY", "MEAT", "VEGETABLES", "FRUITS", "GRAINS", "OTHER"))
        }
    }

    private fun assetExists(context: Context, name: String): Boolean {
        return try {
            context.assets.open(name).close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Maps label strings → [FoodCategory].
     *
     * COCO has a small but useful set of food/produce classes.
     * Everything else maps to [FoodCategory.OTHER] so non-food detections
     * still surface in the UI (useful for debugging before fine-tuning).
     */
    private fun buildFoodCategoryMap() {
        // First, add all the custom/app categories directly
        FoodCategory.entries.forEach { cat ->
            foodCategoryMap[cat.name.lowercase()] = cat
        }

        // food_yolo26n 34-class labels
        foodCategoryMap.apply {
            // FRUITS
            put("apple", FoodCategory.FRUITS)
            put("banana", FoodCategory.FRUITS)
            put("grape", FoodCategory.FRUITS)
            put("lemon", FoodCategory.FRUITS)
            put("orange", FoodCategory.FRUITS)
            put("pineapple", FoodCategory.FRUITS)
            put("watermelon", FoodCategory.FRUITS)

            // VEGETABLES
            put("beans", FoodCategory.VEGETABLES)
            put("chilli", FoodCategory.VEGETABLES)
            put("corn", FoodCategory.VEGETABLES)
            put("tomato", FoodCategory.VEGETABLES)

            // MEAT
            put("fish", FoodCategory.MEAT)

            // DAIRY
            put("milk", FoodCategory.DAIRY)

            // GRAINS
            put("cereal", FoodCategory.GRAINS)
            put("flour", FoodCategory.GRAINS)
            put("pasta", FoodCategory.GRAINS)
            put("rice", FoodCategory.GRAINS)

            // CONDIMENTS
            put("honey", FoodCategory.CONDIMENTS)
            put("jam", FoodCategory.CONDIMENTS)
            put("oil", FoodCategory.CONDIMENTS)
            put("spices", FoodCategory.CONDIMENTS)
            put("sugar", FoodCategory.CONDIMENTS)
            put("tomato_sauce", FoodCategory.CONDIMENTS)
            put("vinegar", FoodCategory.CONDIMENTS)

            // SNACKS
            put("cake", FoodCategory.SNACKS)
            put("candy", FoodCategory.SNACKS)
            put("chips", FoodCategory.SNACKS)
            put("chocolate", FoodCategory.SNACKS)
            put("nuts", FoodCategory.SNACKS)

            // BEVERAGES
            put("coffee", FoodCategory.BEVERAGES)
            put("juice", FoodCategory.BEVERAGES)
            put("soda", FoodCategory.BEVERAGES)
            put("tea", FoodCategory.BEVERAGES)
            put("water", FoodCategory.BEVERAGES)

            // ── crawled_grocery_yolo11n 51-class labels ──────────────────────
            // DAIRY
            put("eggs", FoodCategory.DAIRY)
            put("butter", FoodCategory.DAIRY)
            put("cheese", FoodCategory.DAIRY)
            put("cream", FoodCategory.DAIRY)
            put("yogurt", FoodCategory.DAIRY)

            // MEAT / SEAFOOD
            put("beef_meat", FoodCategory.MEAT)
            put("chicken_meat", FoodCategory.MEAT)
            put("pork_meat", FoodCategory.MEAT)
            put("lamb_meat", FoodCategory.MEAT)
            put("duck_meat", FoodCategory.MEAT)
            put("turkey_meat", FoodCategory.MEAT)
            put("crab_meat", FoodCategory.MEAT)
            put("shrimp", FoodCategory.MEAT)
            put("scallops", FoodCategory.MEAT)
            put("tofu", FoodCategory.MEAT)
            put("cod_fish", FoodCategory.MEAT)
            put("mackerel_fish", FoodCategory.MEAT)
            put("salmon_fish", FoodCategory.MEAT)
            put("tuna_fish", FoodCategory.MEAT)
            put("sardines", FoodCategory.MEAT)
            put("canned_tuna", FoodCategory.MEAT)

            // GRAINS / BREAD
            put("bagel", FoodCategory.GRAINS)
            put("baguette", FoodCategory.GRAINS)
            put("croissant", FoodCategory.GRAINS)
            put("noodles", FoodCategory.GRAINS)
            put("oatmeal", FoodCategory.GRAINS)
            put("tortilla", FoodCategory.GRAINS)
            put("wheat_flour", FoodCategory.GRAINS)
            put("white_bread", FoodCategory.GRAINS)
            put("canned_beans", FoodCategory.GRAINS)

            // VEGETABLES (canned)
            put("canned_corn", FoodCategory.VEGETABLES)
            put("canned_soup", FoodCategory.VEGETABLES)
            put("canned_tomatoes", FoodCategory.VEGETABLES)

            // CONDIMENTS / SAUCES
            put("apple_cider_vinegar", FoodCategory.CONDIMENTS)
            put("bbq_sauce", FoodCategory.CONDIMENTS)
            put("coconut_oil", FoodCategory.CONDIMENTS)
            put("coffee_beans", FoodCategory.BEVERAGES)
            put("curry_paste", FoodCategory.CONDIMENTS)
            put("fish_sauce", FoodCategory.CONDIMENTS)
            put("ketchup", FoodCategory.CONDIMENTS)
            put("mayonnaise", FoodCategory.CONDIMENTS)
            put("mustard", FoodCategory.CONDIMENTS)
            put("olive_oil", FoodCategory.CONDIMENTS)
            put("oyster_sauce", FoodCategory.CONDIMENTS)
            put("rice_vinegar", FoodCategory.CONDIMENTS)
            put("salt", FoodCategory.CONDIMENTS)
            put("soy_sauce", FoodCategory.CONDIMENTS)
            put("tea_bags", FoodCategory.BEVERAGES)
            put("white_sugar", FoodCategory.CONDIMENTS)
            put("black_pepper", FoodCategory.CONDIMENTS)

            // COCO labels that are food-relevant
            put("sandwich", FoodCategory.GRAINS)
            put("hot dog", FoodCategory.MEAT)
            put("pizza", FoodCategory.GRAINS)
            put("donut", FoodCategory.SNACKS)
            put("broccoli", FoodCategory.VEGETABLES)
            put("carrot", FoodCategory.VEGETABLES)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Inference
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Run YOLO26n inference on a [Bitmap] and return a list of [DetectionResult].
     *
     * Returns an empty list if the interpreter is not loaded or inference fails.
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interp = interpreter ?: run {
            AppLog.w(TAG, "detect() called but interpreter is null")
            return emptyList()
        }

        return try {
            val input = preprocessImage(bitmap)

            // Query the actual output shape from the model at runtime so we
            // handle both the primary YOLO26n export and older fallback models.
            val outShape = interp.getOutputTensor(0).shape()
            // outShape = [batch, detections, values]  e.g. [1, 300, 6]
            val numDetections = outShape[1]
            val numValues     = outShape[2]

            val outputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, numDetections, numValues),
                DataType.FLOAT32
            )

            interp.run(input.buffer, outputBuffer.buffer.rewind())

            parseDetections(outputBuffer.floatArray, numDetections, numValues)
        } catch (e: Exception) {
            AppLog.e(TAG, "Inference error", e)
            emptyList()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Pre-processing
    // ────────────────────────────────────────────────────────────────────────

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        // Resize to 640×640, then normalise pixel values from [0, 255] → [0, 1]
        val processor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))   // (pixel - 0) / 255 → [0, 1]
            .add(CastOp(DataType.FLOAT32))
            .build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return processor.process(tensorImage)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Post-processing
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Parse the raw output float array.
     *
     * YOLO26n NMS-free format (6 values per detection):
     *   [x1, y1, x2, y2, confidence, class_id]
     *   Coordinates are absolute pixels in the 640×640 input space.
     *
     * The NMS-free head already suppresses duplicates, so we only need to
     * filter by confidence and clip coordinates to [0, INPUT_SIZE].
     *
     * For legacy fallback models that still use an xywh-centre format the
     * coordinate conversion and an optional safety-net NMS pass is applied.
     */
    private fun parseDetections(
        output: FloatArray,
        numDetections: Int,
        numValues: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val cap = min(numDetections, MAX_DETECTIONS)

        for (i in 0 until cap) {
            val base = i * numValues
            if (base + numValues > output.size) break

            val confidence = output[base + 4]
            if (confidence < CONFIDENCE_THRESHOLD) continue

            val classId = output[base + 5].toInt()
            val label   = if (classId in labels.indices) labels[classId] else "class_$classId"
            val category = foodCategoryMap[label.lowercase()] ?: FoodCategory.OTHER

            val box = if (usingNmsFreeModel) {
                // YOLO26n NMS-free: xyxy absolute coords in [0, 640]
                val x1 = output[base].coerceIn(0f, INPUT_SIZE.toFloat())
                val y1 = output[base + 1].coerceIn(0f, INPUT_SIZE.toFloat())
                val x2 = output[base + 2].coerceIn(0f, INPUT_SIZE.toFloat())
                val y2 = output[base + 3].coerceIn(0f, INPUT_SIZE.toFloat())
                if (x2 <= x1 || y2 <= y1) continue   // degenerate box
                RectF(x1, y1, x2, y2)
            } else {
                // Legacy fallback models: xywh-centre format
                val cx = output[base]
                val cy = output[base + 1]
                val w  = output[base + 2]
                val h  = output[base + 3]
                RectF(
                    max(0f, cx - w / 2f),
                    max(0f, cy - h / 2f),
                    min(INPUT_SIZE.toFloat(), cx + w / 2f),
                    min(INPUT_SIZE.toFloat(), cy + h / 2f)
                )
            }

            results.add(DetectionResult(label, confidence, box, category))
        }

        // The YOLO26n NMS-free head needs no further suppression.
        // apply a lightweight NMS pass.
        return if (usingNmsFreeModel) {
            results.sortedByDescending { it.confidence }
        } else {
            applyNms(results)
        }
    }

    /** Lightweight greedy NMS for legacy fallback model outputs. */
    private fun applyNms(detections: List<DetectionResult>): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val kept   = mutableListOf<DetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            sorted.removeAll { iou(best.boundingBox, it.boundingBox) > IOU_THRESHOLD }
        }

        return kept.take(20)
    }

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

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    fun isModelLoaded(): Boolean = interpreter != null

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Data model
// ────────────────────────────────────────────────────────────────────────────

/**
 * A single object detection result.
 *
 * [boundingBox] is in the model's 640×640 coordinate space.
 * [DetectionOverlayView] scales it to view coordinates.
 */
data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val category: FoodCategory
)
