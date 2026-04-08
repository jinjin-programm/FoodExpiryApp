package com.example.foodexpiryapp.presentation.ui.vision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentVisionScanBinding
import com.example.foodexpiryapp.domain.vision.FoodClassifier
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

@AndroidEntryPoint
class VisionScanFragment : Fragment() {

    companion object {
        private const val TAG = "VisionScanFragment"
        private const val MAX_TOKENS = 24
        private const val TARGET_IMAGE_MAX_SIDE = 224
        private const val PROGRESS_TICK_MS = 400L
    }

    private var _binding: FragmentVisionScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var latestBitmap: Bitmap? = null
    private var isProcessing = false
    private var detectionJob: Job? = null
    private var modelLoadJob: Job? = null
    private var progressTickerJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var foodClassifier: FoodClassifier

    // ML Kit processors removed for true vision

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisionScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        foodClassifier = FoodClassifier(requireContext())
        foodClassifier.initialize()

        updateStatus("Loading Qwen3-VL-2B...", Status.INITIALIZING)
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressDetail.text = "Preparing model..."

        loadModelIfNeeded()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
    }

    private fun loadModelIfNeeded() {
        // TODO: MNN model loading will be added in Phase 5
        binding.progressBar.visibility = View.GONE
        binding.tvProgressDetail.visibility = View.GONE
        binding.tvInstruction.visibility = View.VISIBLE
        updateStatus("AI model not available (MNN upgrade pending)", Status.READY)
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCapture.setOnClickListener {
            captureAndAnalyze()
        }

        binding.btnCancelInference.setOnClickListener {
            cancelOngoingInference()
        }

        binding.btnAskAi.setOnClickListener {
            runAskAiInference()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val bm = imageProxy.toBitmap()
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        if (bm != null && rotation != 0) {
                            val matrix = android.graphics.Matrix()
                            matrix.postRotate(rotation.toFloat())
                            latestBitmap = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
                        } else {
                            latestBitmap = bm
                        }
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureAndAnalyze() {
        if (isProcessing) return

        var bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Crop the bitmap to exactly what's inside the UI bounding box
        bitmap = cropToBoundingBox(bitmap)

        // Quick Scan Flow
        if (foodClassifier.isInitialized()) {
            val result = foodClassifier.classify(bitmap)
            if (result != null) {
                displayQuickScanResult(result)
                return
            }
        }
        
        // Fallback to Ask AI if Quick Scan fails or not initialized
        runAskAiInference(bitmap)
    }

    private fun cropToBoundingBox(bitmap: Bitmap): Bitmap {
        try {
            val previewWidth = binding.previewView.width.toFloat()
            val previewHeight = binding.previewView.height.toFloat()
            if (previewWidth == 0f || previewHeight == 0f) return bitmap

            // UI Bounding box coordinates and dimensions
            val boxX = binding.simulatedBox.x
            val boxY = binding.simulatedBox.y
            val boxWidth = binding.simulatedBox.width.toFloat()
            val boxHeight = binding.simulatedBox.height.toFloat()

            // Captured upright bitmap dimensions
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            // PreviewView defaults to FILL_CENTER. We calculate the scale factor.
            val scale = maxOf(previewWidth / bitmapWidth, previewHeight / bitmapHeight)

            // Scaled bitmap dimensions
            val scaledBitmapWidth = bitmapWidth * scale
            val scaledBitmapHeight = bitmapHeight * scale

            // Calculate offset of the scaled bitmap relative to the preview (since it's centered)
            val offsetX = (scaledBitmapWidth - previewWidth) / 2f
            val offsetY = (scaledBitmapHeight - previewHeight) / 2f

            // Map box coordinates to scaled bitmap coordinates
            val mappedBoxX = boxX + offsetX
            val mappedBoxY = boxY + offsetY

            // Map back to original bitmap coordinates
            val cropX = (mappedBoxX / scale).toInt().coerceAtLeast(0)
            val cropY = (mappedBoxY / scale).toInt().coerceAtLeast(0)
            val cropWidth = (boxWidth / scale).toInt().coerceAtMost(bitmap.width - cropX)
            val cropHeight = (boxHeight / scale).toInt().coerceAtMost(bitmap.height - cropY)

            if (cropWidth <= 0 || cropHeight <= 0) return bitmap

            return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop bitmap", e)
            return bitmap
        }
    }

    private fun displayQuickScanResult(result: com.example.foodexpiryapp.domain.vision.ClassificationResult) {
        binding.resultCard.visibility = View.VISIBLE
        binding.rawResponseCard.visibility = View.GONE
        
        binding.tvFoodName.text = result.category.nameTw
        binding.tvExpiryDate.text = "建議保存: ${result.category.displayDays}"
        
        binding.tvConfidence.visibility = View.VISIBLE
        val confPct = (result.confidence * 100).toInt()
        binding.tvConfidence.text = "Confidence: $confPct% - ${result.category.description}"
        
        binding.btnAskAi.visibility = View.VISIBLE
        
        // Suggest AI if confidence is low
        if (result.confidence < 0.6f) {
            Toast.makeText(context, "信心較低，建議點擊「AI 深度分析」", Toast.LENGTH_LONG).show()
        }
        updateStatus("Quick Scan complete", Status.READY)
    }

    private fun runAskAiInference(customBitmap: Bitmap? = null) {
        if (isProcessing) return

        // TODO: MNN AI inference will be added in Phase 5
        Toast.makeText(context, "AI analysis is temporarily unavailable — MNN upgrade pending", Toast.LENGTH_LONG).show()
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        val start = System.currentTimeMillis()
        progressTickerJob = scope.launch {
            while (isActive && isProcessing) {
                val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
                val etaSec = when {
                    elapsedSec < 3 -> 8.0
                    elapsedSec < 8 -> 5.0
                    else -> max(2.0, 10.0 - elapsedSec)
                }
                updateProgress("Processing... ${"%.1f".format(elapsedSec)}s elapsed, ~${"%.0f".format(etaSec)}s remaining")
                delay(PROGRESS_TICK_MS)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    private fun cancelOngoingInference() {
        if (!isProcessing) return
        detectionJob?.cancel()
        stopProgressTicker()
        isProcessing = false
        binding.progressBar.visibility = View.GONE
        binding.btnCancelInference.visibility = View.GONE
        binding.btnCapture.isEnabled = true
        binding.tvInstruction.visibility = View.VISIBLE
        binding.tvProgressDetail.visibility = View.VISIBLE
        binding.tvProgressDetail.text = "Cancelled"
        Toast.makeText(requireContext(), "Inference cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun resizeForVision(source: Bitmap, maxSide: Int): Bitmap {
        val width = source.width
        val height = source.height
        val longest = max(width, height)
        if (longest <= maxSide) return source

        val scale = maxSide.toFloat() / longest.toFloat()
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun updateProgress(message: String) {
        _binding?.let { binding ->
            binding.tvProgressDetail.text = message
        }
    }

    private data class FoodResult(
        val foodName: String,
        val expiryDate: String?,
        val rawResponse: String
    )

    private fun parseFoodResponse(response: String): FoodResult {
        val foodRegex = Regex("""FOOD:\s*\[?([^\[\]\n]+)\]?"?""", RegexOption.IGNORE_CASE)
        val foodMatch = foodRegex.find(response)
        if (foodMatch != null) {
            val foodName = foodMatch.groupValues[1].trim()
            val expiryRegex = Regex("""EXPIRY:\s*\[?([^\[\]\n]+)\]?"?""", RegexOption.IGNORE_CASE)
            val expiryMatch = expiryRegex.find(response)
            val expiryDate = expiryMatch?.groupValues?.get(1)?.trim()
            val cleanExpiry = expiryDate?.takeIf { 
                !it.equals("not visible", ignoreCase = true) && 
                !it.equals("not shown", ignoreCase = true) &&
                it.isNotBlank() 
            }
            return FoodResult(foodName, cleanExpiry, response)
        }
        
        val clean = response.trim()
        val foodName = if (clean.length < 40 && clean.isNotBlank()) {
            clean.removeSuffix(".").removeSuffix("!").trim()
        } else {
            val lines = response.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val firstLine = lines.firstOrNull()
            if (firstLine != null && firstLine.length < 40) {
                firstLine.removeSuffix(".").removeSuffix("!").trim()
            } else {
                response.take(30).trim()
            }
        }.ifBlank { "Unknown" }
        
        return FoodResult(foodName, null, response)
    }

    private fun displayAiResult(foodName: String, expiryDate: String?, rawResponse: String) {
        binding.tvFoodName.text = "AI Result: $foodName"
        binding.tvExpiryDate.text = "Expiry: ${expiryDate ?: "Not detected"}"
        binding.tvConfidence.visibility = View.GONE
        binding.tvRawResponse.text = rawResponse
        
        binding.resultCard.visibility = View.VISIBLE
        binding.rawResponseCard.visibility = View.VISIBLE
        updateStatus("AI Analysis complete", Status.READY)
    }

    private enum class Status {
        INITIALIZING, READY, ANALYZING, ERROR
    }

    private fun updateStatus(message: String, status: Status = Status.READY) {
        _binding?.let { binding ->
            binding.tvStatus.text = message
            
            val color = when (status) {
                Status.INITIALIZING -> android.graphics.Color.BLUE
                Status.READY -> android.graphics.Color.GREEN
                Status.ANALYZING -> android.graphics.Color.YELLOW
                Status.ERROR -> android.graphics.Color.RED
            }
            binding.viewStatusIndicator.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detectionJob?.cancel()
        modelLoadJob?.cancel()
        progressTickerJob?.cancel()
        scope.cancel()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        if (::foodClassifier.isInitialized) {
            foodClassifier.close()
        }
        
        _binding = null
    }
}

private fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

// ML Kit Tasks utility (for awaiting results)
object Tasks {
    fun <TResult : Any?> await(task: com.google.android.gms.tasks.Task<TResult>): TResult? {
        var result: TResult? = null
        var exception: Exception? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        
        task.addOnSuccessListener {
            result = it
            latch.countDown()
        }.addOnFailureListener {
            exception = it
            latch.countDown()
        }
        
        latch.await()
        
        if (exception != null) {
            throw exception!!
        }
        
        return result
    }
}
