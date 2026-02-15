package com.example.foodexpiryapp.presentation.ui.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
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
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * YOLO object detector for food item recognition.
 * Uses TensorFlow Lite to detect food items from camera images.
 */
class YoloDetector(
    context: Context,
    private val modelName: String = DEFAULT_MODEL_NAME
) {

    companion object {
        private const val TAG = "YoloDetector"
        // FoodVision YOLOv8 model (55 food classes - Fruits & Vegetables)
        const val DEFAULT_MODEL_NAME = "foodvision_yolov8.tflite"
        // Fallback models
        const val ALTERNATIVE_MODELS = "food_yolov8_roboflow.tflite,food_yolov8n_float32.tflite,yolo11n_float32.tflite"
        private const val LABELS_FILE = "yolo_labels.txt"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.45f
        private const val IOU_THRESHOLD = 0.5f
        private const val MAX_DETECTIONS = 20
    }

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()
    private val foodCategoryMapping = mutableMapOf<String, FoodCategory>()

    init {
        try {
            loadModel(context)
            loadLabels(context)
            initializeFoodCategoryMapping()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing YOLO detector", e)
        }
    }

    private fun loadModel(context: Context) {
        val modelsToTry = listOf(modelName) + ALTERNATIVE_MODELS.split(",")
        
        for (modelFile in modelsToTry) {
            try {
                val model = FileUtil.loadMappedFile(context, modelFile.trim())
                val options = Interpreter.Options().apply {
                    numThreads = 4
                }
                interpreter = Interpreter(model, options)
                Log.d(TAG, "YOLO model loaded successfully: $modelFile")
                return
            } catch (e: IOException) {
                Log.w(TAG, "Model not found: $modelFile")
                // Try next model
            }
        }
        
        Log.e(TAG, "No YOLO model found. Tried: ${modelsToTry.joinToString()}")
        Log.e(TAG, "Please export the model. See YOLO_SETUP.md for instructions.")
    }

    private fun loadLabels(context: Context) {
        try {
            context.assets.open(LABELS_FILE).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { labels.add(it.trim()) }
                }
            }
            Log.d(TAG, "Loaded ${labels.size} labels")
        } catch (e: IOException) {
            Log.e(TAG, "Error loading labels: ${e.message}")
            // Use default COCO labels for common food items
            loadDefaultLabels()
        }
    }

    private fun loadDefaultLabels() {
        // Default COCO dataset labels relevant to food
        val defaultLabels = listOf(
            "apple", "banana", "orange", "broccoli", "carrot",
            "pizza", "donut", "cake", "sandwich", "hot dog",
            "bottle", "cup", "bowl", "wine glass", "fork",
            "knife", "spoon", "frisbee"
        )
        labels.addAll(defaultLabels)
    }

    private fun initializeFoodCategoryMapping() {
        // Map YOLO detected items to FoodCategories
        // Using FoodVision YOLOv8 model with 55 food classes (Fruits & Vegetables focus)
        foodCategoryMapping.apply {
            // Fruits (26 classes)
            put("avocados", FoodCategory.FRUITS)
            put("green_apples", FoodCategory.FRUITS)
            put("green_grapes", FoodCategory.FRUITS)
            put("kiwifruit", FoodCategory.FRUITS)
            put("limes", FoodCategory.FRUITS)
            put("banana", FoodCategory.FRUITS)
            put("date", FoodCategory.FRUITS)
            put("peach", FoodCategory.FRUITS)
            put("pear", FoodCategory.FRUITS)
            put("blackberry", FoodCategory.FRUITS)
            put("blueberry", FoodCategory.FRUITS)
            put("cherry", FoodCategory.FRUITS)
            put("plum", FoodCategory.FRUITS)
            put("purple grapes", FoodCategory.FRUITS)
            put("raspberry", FoodCategory.FRUITS)
            put("red apple", FoodCategory.FRUITS)
            put("red grape", FoodCategory.FRUITS)
            put("strawberry", FoodCategory.FRUITS)
            put("watermelon", FoodCategory.FRUITS)
            put("apricot", FoodCategory.FRUITS)
            put("grapefruit", FoodCategory.FRUITS)
            put("lemon", FoodCategory.FRUITS)
            put("mango", FoodCategory.FRUITS)
            put("nectarine", FoodCategory.FRUITS)
            put("orange", FoodCategory.FRUITS)
            put("pineapple", FoodCategory.FRUITS)

            // Vegetables (29 classes)
            put("asparagus", FoodCategory.VEGETABLES)
            put("broccoli", FoodCategory.VEGETABLES)
            put("cabbage", FoodCategory.VEGETABLES)
            put("celery", FoodCategory.VEGETABLES)
            put("cucumber", FoodCategory.VEGETABLES)
            put("green_beans", FoodCategory.VEGETABLES)
            put("green_capsicum", FoodCategory.VEGETABLES)
            put("lettuce", FoodCategory.VEGETABLES)
            put("peas", FoodCategory.VEGETABLES)
            put("spinach", FoodCategory.VEGETABLES)
            put("cauliflower", FoodCategory.VEGETABLES)
            put("garlic", FoodCategory.VEGETABLES)
            put("ginger", FoodCategory.VEGETABLES)
            put("mushroom", FoodCategory.VEGETABLES)
            put("onion", FoodCategory.VEGETABLES)
            put("parsnip", FoodCategory.VEGETABLES)
            put("potato", FoodCategory.VEGETABLES)
            put("turnip", FoodCategory.VEGETABLES)
            put("beetroot", FoodCategory.VEGETABLES)
            put("eggplant", FoodCategory.VEGETABLES)
            put("purple asparagus", FoodCategory.VEGETABLES)
            put("radish", FoodCategory.VEGETABLES)
            put("red cabbage", FoodCategory.VEGETABLES)
            put("red capsicum", FoodCategory.VEGETABLES)
            put("tomato", FoodCategory.VEGETABLES)
            put("carrot", FoodCategory.VEGETABLES)
            put("corn", FoodCategory.VEGETABLES)
            put("pumpkin", FoodCategory.VEGETABLES)
            put("sweet_potato", FoodCategory.VEGETABLES)
        }
    }

    /**
     * Detects food items in the given bitmap image.
     * Returns a list of detection results.
     */
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        interpreter?.let { interpreter ->
            try {
                // Preprocess the image
                val tensorImage = preprocessImage(bitmap)
                
                // Run inference
                val inputBuffer = tensorImage.buffer
                val outputBuffer = TensorBuffer.createFixedSize(
                    intArrayOf(1, MAX_DETECTIONS, 6),
                    DataType.FLOAT32
                )
                
                interpreter.run(inputBuffer, outputBuffer.buffer.rewind())
                
                // Parse results
                return parseDetections(outputBuffer.floatArray)
            } catch (e: Exception) {
                Log.e(TAG, "Error during detection", e)
            }
        }
        
        // Fallback: return empty list if model not available
        return emptyList()
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .add(CastOp(DataType.FLOAT32))
            .build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        return imageProcessor.process(tensorImage)
    }

    private fun parseDetections(outputArray: FloatArray): List<DetectionResult> {
        val detections = mutableListOf<DetectionResult>()
        
        // YOLO output format: [x, y, w, h, confidence, class]
        for (i in 0 until min(MAX_DETECTIONS, outputArray.size / 6)) {
            val offset = i * 6
            val confidence = outputArray[offset + 4]
            
            if (confidence > CONFIDENCE_THRESHOLD) {
                val x = outputArray[offset]
                val y = outputArray[offset + 1]
                val w = outputArray[offset + 2]
                val h = outputArray[offset + 3]
                val classId = outputArray[offset + 5].toInt()
                
                val label = if (classId < labels.size) labels[classId] else "unknown"
                val category = foodCategoryMapping[label.lowercase()] ?: FoodCategory.OTHER
                
                val boundingBox = RectF(
                    max(0f, x - w / 2),
                    max(0f, y - h / 2),
                    min(INPUT_SIZE.toFloat(), x + w / 2),
                    min(INPUT_SIZE.toFloat(), y + h / 2)
                )
                
                detections.add(
                    DetectionResult(
                        label = label,
                        confidence = confidence,
                        boundingBox = boundingBox,
                        category = category
                    )
                )
            }
        }
        
        return applyNMS(detections)
    }

    /**
     * Apply Non-Maximum Suppression to remove overlapping detections
     */
    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        
        val sortedDetections = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionResult>()
        
        while (sortedDetections.isNotEmpty()) {
            val current = sortedDetections.removeAt(0)
            selected.add(current)
            
            sortedDetections.removeAll { detection ->
                calculateIoU(current.boundingBox, detection.boundingBox) > IOU_THRESHOLD
            }
        }
        
        return selected.take(5) // Return top 5 detections
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val x1 = max(box1.left, box2.left)
        val y1 = max(box1.top, box2.top)
        val x2 = min(box1.right, box2.right)
        val y2 = min(box1.bottom, box2.bottom)
        
        if (x2 < x1 || y2 < y1) return 0f
        
        val intersection = (x2 - x1) * (y2 - y1)
        val area1 = (box1.right - box1.left) * (box1.bottom - box1.top)
        val area2 = (box2.right - box2.left) * (box2.bottom - box2.top)
        val union = area1 + area2 - intersection
        
        return if (union > 0) intersection / union else 0f
    }

    fun isModelLoaded(): Boolean = interpreter != null

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

/**
 * Result of a YOLO detection
 */
data class DetectionResult(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val category: FoodCategory
)
