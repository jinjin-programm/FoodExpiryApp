package com.example.foodexpiryapp.domain.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.example.foodexpiryapp.util.AppLog
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.channels.FileChannel

data class ClassificationResult(
    val category: FoodCategory,
    val confidence: Float,
    val inferenceTimeMs: Long
)

data class FoodCategory(
    val id: String,
    val nameTw: String,
    val minDays: Int,
    val maxDays: Int,
    val description: String
) {
    val displayDays: String
        get() = if (minDays > 0) "$minDays - $maxDays days" else "--"
}

class FoodClassifier(private val context: Context) {

    companion object {
        private const val TAG = "FoodClassifier"
        private const val MODEL_FILE = "food_efficientnet_lite0_int8.tflite"
        private const val INPUT_SIZE = 224
    }

    private var interpreter: Interpreter? = null

    // Pre-defined categories based on our 15-class split
    // Note: The order must match the output classes of the trained TFLite model perfectly.
    // For now, we use a mapping to ID so we can look it up if the model provides labels,
    // or just assume a fixed order once trained.
    private val categories = mapOf(
        "leafy_vegetables" to FoodCategory("leafy_vegetables", "Leafy Vegetables", 3, 5, "Loses moisture quickly; consume soon"),
        "root_vegetables" to FoodCategory("root_vegetables", "Root Vegetables", 14, 30, "e.g. carrots, potatoes (room temp or refrigerated)"),
        "gourd_fruit_vegetables" to FoodCategory("gourd_fruit_vegetables", "Gourd & Fruit Vegetables", 5, 7, "e.g. cucumber, tomato, pumpkin"),
        "hard_fruits" to FoodCategory("hard_fruits", "Hard Fruits", 10, 14, "e.g. apples, guava"),
        "soft_fruits" to FoodCategory("soft_fruits", "Soft Fruits", 3, 5, "e.g. strawberries, grapes, peaches"),
        "tropical_fruits" to FoodCategory("tropical_fruits", "Tropical Fruits", 5, 7, "e.g. bananas, mangoes (ripen at room temp, then refrigerate)"),
        "fresh_meat" to FoodCategory("fresh_meat", "Fresh Meat", 2, 3, "Frozen storage lasts months; this is the refrigerated standard"),
        "processed_meat" to FoodCategory("processed_meat", "Processed Meat", 7, 14, "e.g. sausages, bacon (unopened)"),
        "seafood" to FoodCategory("seafood", "Seafood", 1, 2, "Highly perishable; consume or freeze promptly"),
        "eggs" to FoodCategory("eggs", "Eggs", 21, 30, "Store deep inside the refrigerator"),
        "dairy_products" to FoodCategory("dairy_products", "Dairy Products", 7, 10, "Varies by milk or cheese type; average value"),
        "tofu_products" to FoodCategory("tofu_products", "Tofu Products", 3, 5, "Keep submerged in water and refrigerate; change water frequently"),
        "grains_dry_goods" to FoodCategory("grains_dry_goods", "Grains & Dry Goods", 90, 180, "Seal and store at room temp or refrigerate to prevent pests"),
        "others" to FoodCategory("others", "Others", 0, 0, "Enter manually or use 'Ask AI' for further analysis")
    )

    // Example mapping order based on sorted folder names. 
    // MUST match `class_names` output in the Python training script.
    val classLabels = listOf(
        "dairy_products",
        "eggs",
        "fresh_meat",
        "gourd_fruit_vegetables",
        "grains_dry_goods",
        "hard_fruits",
        "leafy_vegetables",
        "others",
        "processed_meat",
        "root_vegetables",
        "seafood",
        "soft_fruits",
        "tofu_products",
        "tropical_fruits"
    )

    fun initialize() {
        try {
            val options = Interpreter.Options().apply {
                // Enable NNAPI if available for maximum performance
                setUseNNAPI(true)
                // Fallback to CPU threads
                numThreads = 4
            }
            interpreter = Interpreter(loadModelFile(), options)
            AppLog.i(TAG, "TFLite model initialized successfully")
        } catch (e: Exception) {
            AppLog.e(TAG, "Error initializing TFLite model", e)
            // Model file probably doesn't exist yet in assets, which is expected during dev
        }
    }

    fun isInitialized() = interpreter != null

    private fun loadModelFile(): java.nio.MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    fun classify(bitmap: Bitmap): ClassificationResult? {
        val tflite = interpreter ?: return null

        try {
            val startTime = SystemClock.uptimeMillis()

            // 1. Prepare Image
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                // Normalization depends on your model. EfficientNet-Lite expects [0, 1] usually
                // but INT8 might expect [0, 255] or [-1, 1]. Adjust as needed based on training.
                // Assuming UINT8 model where inputs are [0, 255]:
                .build()

            var tensorImage = TensorImage(DataType.UINT8)
            tensorImage.load(bitmap)
            tensorImage = imageProcessor.process(tensorImage)

            // 2. Prepare Output Buffer
            val probabilityBuffer = TensorBuffer.createFixedSize(intArrayOf(1, classLabels.size), DataType.UINT8)

            // 3. Run Inference
            tflite.run(tensorImage.buffer, probabilityBuffer.buffer.rewind())

            // 4. Parse Results (UINT8 mapped to 0..255, so we divide by 255 to get confidence 0..1)
            val probabilities = probabilityBuffer.intArray
            var maxIdx = -1
            var maxProb = -1f

            for (i in probabilities.indices) {
                val prob = (probabilities[i] and 0xFF) / 255f
                if (prob > maxProb) {
                    maxProb = prob
                    maxIdx = i
                }
            }

            val inferenceTime = SystemClock.uptimeMillis() - startTime
            
            val classId = if (maxIdx in classLabels.indices) classLabels[maxIdx] else "others"
            val category = categories[classId] ?: categories["others"]!!

            AppLog.d(TAG, "Classification: ${category.nameTw} (confidence: $maxProb) in ${inferenceTime}ms")

            return ClassificationResult(category, maxProb, inferenceTime)

        } catch (e: Exception) {
            AppLog.e(TAG, "Error running inference", e)
            return null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
