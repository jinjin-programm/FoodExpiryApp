package com.example.foodexpiryapp.domain.vision

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
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
        get() = if (minDays > 0) "$minDays - $maxDays 天" else "--"
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
        "leafy_vegetables" to FoodCategory("leafy_vegetables", "葉菜類", 3, 5, "容易流失水分，需盡快食用"),
        "root_vegetables" to FoodCategory("root_vegetables", "根莖類", 14, 30, "如紅蘿蔔、馬鈴薯（可常溫或冷藏）"),
        "gourd_fruit_vegetables" to FoodCategory("gourd_fruit_vegetables", "瓜果類", 5, 7, "如小黃瓜、番茄、南瓜"),
        "hard_fruits" to FoodCategory("hard_fruits", "硬水果", 10, 14, "如蘋果、芭樂"),
        "soft_fruits" to FoodCategory("soft_fruits", "軟水果", 3, 5, "如草莓、葡萄、水蜜桃"),
        "tropical_fruits" to FoodCategory("tropical_fruits", "熱帶水果", 5, 7, "如香蕉、芒果（通常建議先常溫後冷藏）"),
        "fresh_meat" to FoodCategory("fresh_meat", "新鮮肉類", 2, 3, "若放冷凍可達數月，此為冷藏標準"),
        "processed_meat" to FoodCategory("processed_meat", "加工肉類", 7, 14, "如香腸、培根（未開封）"),
        "seafood" to FoodCategory("seafood", "海鮮類", 1, 2, "極易腐敗，建議盡速食用或冷凍"),
        "eggs" to FoodCategory("eggs", "雞蛋", 21, 30, "需放置於冰箱深處冷藏"),
        "dairy_products" to FoodCategory("dairy_products", "乳製品", 7, 10, "依鮮奶或起司種類而異，取平均值"),
        "tofu_products" to FoodCategory("tofu_products", "豆腐製品", 3, 5, "需泡水冷藏並勤換水"),
        "grains_dry_goods" to FoodCategory("grains_dry_goods", "穀物乾貨", 90, 180, "建議密封常溫或冷藏防蟲"),
        "others" to FoodCategory("others", "其他", 0, 0, "提示用戶手動輸入或使用「Ask AI」進一步分析")
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
            Log.i(TAG, "TFLite model initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TFLite model", e)
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

            Log.d(TAG, "Classification: ${category.nameTw} (confidence: $maxProb) in ${inferenceTime}ms")

            return ClassificationResult(category, maxProb, inferenceTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error running inference", e)
            return null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
