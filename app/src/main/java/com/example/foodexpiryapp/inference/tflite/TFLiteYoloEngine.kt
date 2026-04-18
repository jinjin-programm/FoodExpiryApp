package com.example.foodexpiryapp.inference.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.foodexpiryapp.domain.model.DetectionResult
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.inference.mnn.ModelLifecycleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TFLiteYoloEngine @Inject constructor(
    private val lifecycleManager: ModelLifecycleManager,
    @ApplicationContext private val context: Context
) : YoloDetector {

    companion object {
        private const val TAG = "TFLiteYoloEngine"
        private const val MODEL_PATH = "food_yolo26n_float32.tflite"
        private const val LABELS_PATH = "yolo_labels.txt"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val MAX_DETECTIONS = 8
        private const val IOU_THRESHOLD = 0.45f
    }

    private var interpreter: org.tensorflow.lite.Interpreter? = null
    private var isLoaded = false
    private var labels: List<String> = emptyList()

    override suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext true

        if (!lifecycleManager.acquire(ModelLifecycleManager.ModelType.YOLO)) {
            Log.e(TAG, "Cannot acquire YOLO lifecycle")
            return@withContext false
        }

        try {
            val modelBuffer = loadModelFile(MODEL_PATH)
            val options = org.tensorflow.lite.Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = org.tensorflow.lite.Interpreter(modelBuffer, options)
            labels = loadLabels(LABELS_PATH)
            isLoaded = true
            Log.d(TAG, "TFLite YOLO model loaded (${labels.size} labels)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TFLite YOLO model", e)
            lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
            false
        }
    }

    override fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (!isLoaded || interpreter == null) {
            Log.w(TAG, "detect() called but model not loaded")
            return emptyList()
        }

        return try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = convertBitmapToByteBuffer(resized)

            val outputBoxes = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
            val outputScores = Array(1) { FloatArray(MAX_DETECTIONS) }
            val outputClasses = Array(1) { FloatArray(MAX_DETECTIONS) }

            val outputMap = mapOf(
                0 to outputBoxes,
                1 to outputScores,
                2 to outputClasses
            )

            interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

            val scaleX = originalWidth.toFloat() / INPUT_SIZE
            val scaleY = originalHeight.toFloat() / INPUT_SIZE

            val results = mutableListOf<DetectionResult>()
            for (i in 0 until MAX_DETECTIONS) {
                val score = outputScores[0][i]
                if (score < CONFIDENCE_THRESHOLD) continue

                val x1 = outputBoxes[0][i][0] * scaleX
                val y1 = outputBoxes[0][i][1] * scaleY
                val x2 = outputBoxes[0][i][2] * scaleX
                val y2 = outputBoxes[0][i][3] * scaleY

                val classId = outputClasses[0][i].toInt()
                val label = if (classId in labels.indices) labels[classId] else "unknown"

                results.add(
                    DetectionResult(
                        id = results.size,
                        boundingBox = RectF(x1, y1, x2, y2),
                        label = label,
                        category = mapLabelToCategory(label),
                        confidence = score
                    )
                )
            }

            applyNms(results)
        } catch (e: Exception) {
            Log.e(TAG, "YOLO detection error", e)
            emptyList()
        }
    }

    override suspend fun unloadModel() = withContext(Dispatchers.IO) {
        try {
            interpreter?.close()
            Log.d(TAG, "TFLite YOLO model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Error unloading model", e)
        }
        interpreter = null
        isLoaded = false
        lifecycleManager.release(ModelLifecycleManager.ModelType.YOLO)
    }

    override fun isModelLoaded(): Boolean = isLoaded

    private fun loadModelFile(path: String): ByteBuffer {
        val assetFd = context.assets.openFd(path)
        val inputStream = assetFd.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFd.startOffset
        val declaredLength = assetFd.declaredLength
        return fileChannel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }

    private fun loadLabels(path: String): List<String> {
        val reader = BufferedReader(InputStreamReader(context.assets.open(path)))
        return reader.use { it.readLines() }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16) and 0xFF) / 255.0f)
                byteBuffer.putFloat(((`val` shr 8) and 0xFF) / 255.0f)
                byteBuffer.putFloat((`val` and 0xFF) / 255.0f)
            }
        }
        return byteBuffer
    }

    private fun applyNms(detections: List<DetectionResult>): List<DetectionResult> {
        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<DetectionResult>()
        val suppressed = BooleanArray(sorted.size)

        for (i in sorted.indices) {
            if (suppressed[i]) continue
            selected.add(sorted[i])
            for (j in i + 1 until sorted.indices.count { true }) {
                if (j < suppressed.size && !suppressed[j]) {
                    val iou = computeIoU(sorted[i].boundingBox, sorted[j].boundingBox)
                    if (iou > IOU_THRESHOLD) {
                        suppressed[j] = true
                    }
                }
            }
        }

        return selected.take(MAX_DETECTIONS)
    }

    private fun computeIoU(a: RectF, b: RectF): Float {
        val intersectLeft = maxOf(a.left, b.left)
        val intersectTop = maxOf(a.top, b.top)
        val intersectRight = minOf(a.right, b.right)
        val intersectBottom = minOf(a.bottom, b.bottom)

        val intersectArea = maxOf(0f, intersectRight - intersectLeft) *
                maxOf(0f, intersectBottom - intersectTop)
        if (intersectArea <= 0f) return 0f

        val areaA = a.width() * a.height()
        val areaB = b.width() * b.height()
        return intersectArea / (areaA + areaB - intersectArea)
    }

    private fun mapLabelToCategory(label: String): FoodCategory {
        val lower = label.lowercase()
        return when {
            lower.contains("meat") || lower.contains("chicken") ||
                    lower.contains("beef") || lower.contains("pork") ||
                    lower.contains("lamb") || lower.contains("duck") ||
                    lower.contains("turkey") -> FoodCategory.MEAT

            lower.contains("fish") || lower.contains("salmon") ||
                    lower.contains("tuna") || lower.contains("shrimp") ||
                    lower.contains("crab") || lower.contains("cod") ||
                    lower.contains("mackerel") || lower.contains("sardine") ||
                    lower.contains("scallop") -> FoodCategory.MEAT

            lower.contains("milk") || lower.contains("cheese") ||
                    lower.contains("yogurt") || lower.contains("cream") ||
                    lower.contains("butter") || lower.contains("egg") -> FoodCategory.DAIRY

            lower.contains("apple") || lower.contains("banana") ||
                    lower.contains("orange") || lower.contains("grape") ||
                    lower.contains("strawberry") || lower.contains("mango") ||
                    lower.contains("lemon") || lower.contains("pineapple") ||
                    lower.contains("watermelon") || lower.contains("peach") ||
                    lower.contains("pear") || lower.contains("cherry") ||
                    lower.contains("plum") || lower.contains("berry") ||
                    lower.contains("kiwi") || lower.contains("lime") ||
                    lower.contains("grapefruit") || lower.contains("apricot") ||
                    lower.contains("nectarine") || lower.contains("avocado") ||
                    lower.contains("coconut") -> FoodCategory.FRUITS

            lower.contains("tomato") || lower.contains("potato") ||
                    lower.contains("onion") || lower.contains("carrot") ||
                    lower.contains("broccoli") || lower.contains("cabbage") ||
                    lower.contains("lettuce") || lower.contains("spinach") ||
                    lower.contains("cucumber") || lower.contains("celery") ||
                    lower.contains("pepper") || lower.contains("corn") ||
                    lower.contains("mushroom") || lower.contains("garlic") ||
                    lower.contains("ginger") || lower.contains("asparagus") ||
                    lower.contains("beans") || lower.contains("peas") ||
                    lower.contains("beetroot") || lower.contains("radish") ||
                    lower.contains("pumpkin") || lower.contains("turnip") ||
                    lower.contains("parsnip") || lower.contains("capsicum") ||
                    lower.contains("eggplant") || lower.contains("sweet_potato") -> FoodCategory.VEGETABLES

            lower.contains("bread") || lower.contains("bagel") ||
                    lower.contains("baguette") || lower.contains("croissant") ||
                    lower.contains("tortilla") || lower.contains("noodle") ||
                    lower.contains("pasta") || lower.contains("flour") -> FoodCategory.GRAINS

            lower.contains("sauce") || lower.contains("oil") ||
                    lower.contains("vinegar") || lower.contains("ketchup") ||
                    lower.contains("mayonnaise") || lower.contains("mustard") ||
                    lower.contains("honey") || lower.contains("jam") ||
                    lower.contains("sugar") || lower.contains("salt") ||
                    lower.contains("pepper") || lower.contains("spice") -> FoodCategory.CONDIMENTS

            else -> FoodCategory.OTHER
        }
    }
}
