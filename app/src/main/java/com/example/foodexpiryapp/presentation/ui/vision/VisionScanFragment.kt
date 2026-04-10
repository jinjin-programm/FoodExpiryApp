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
import com.example.foodexpiryapp.domain.usecase.IdentifyFoodUseCase
import com.example.foodexpiryapp.data.remote.ModelDownloadManager
import com.example.foodexpiryapp.domain.model.DownloadUiState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
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

    @Inject lateinit var identifyFoodUseCase: IdentifyFoodUseCase
    @Inject lateinit var modelDownloadManager: ModelDownloadManager

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

        updateStatus("Loading Qwen3-VL-2B...", Status.INITIALIZING)

        loadModelIfNeeded()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
    }

    private fun loadModelIfNeeded() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Check if model is ready
            var isReady = false
            try {
                isReady = modelDownloadManager.isModelReady()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking model status", e)
            }

            if (isReady) {
                updateStatus("AI model ready", Status.READY)
            } else {
                // Check download state
                modelDownloadManager.observeDownloadState().collect { state ->
                    when (state) {
                        is DownloadUiState.Ready,
                        is DownloadUiState.Complete -> {
                            updateStatus("AI model ready", Status.READY)
                        }
                        is DownloadUiState.NotDownloaded -> {
                            updateStatus("AI model not downloaded — tap 'AI 深度分析' to download", Status.READY)
                        }
                        is DownloadUiState.Downloading -> {
                            updateStatus("Downloading model... ${state.overallProgress}%", Status.INITIALIZING)
                        }
                        is DownloadUiState.Error -> {
                            updateStatus("Download error: ${state.message}", Status.ERROR)
                        }
                        else -> {
                            updateStatus("AI model ${state::class.simpleName}", Status.INITIALIZING)
                        }
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCapture.setOnClickListener {
            captureAndAnalyze()
        }

        binding.btnCancelProgress.setOnClickListener {
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
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val bm = imageProxy.toBitmap()
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        if (rotation != 0) {
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

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return
        }

        // D-08: White flash animation (100-150ms)
        showFlashAnimation {
            // D-09: Freeze frame handled by stopping camera feed — but since we already
            // captured latestBitmap, the "frozen frame" is the bitmap we have.
            // Show progress overlay on top of the frozen camera preview.

            // No more cropToBoundingBox — simulated_box is removed (D-05)
            // Directly run Ask AI flow (TFLite FoodClassifier removed in MNN migration)
            runAskAiInference(bitmap)
        }
    }

    // D-08: White flash animation helper
    private fun showFlashAnimation(onComplete: () -> Unit) {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 1f

        // D-08: 100-150ms white flash
        binding.flashOverlay.animate()
            .alpha(0f)
            .setDuration(150L)
            .withEndAction {
                binding.flashOverlay.visibility = View.GONE
                // D-09: Show progress overlay after flash
                showProgressOverlay()
                onComplete()
            }
            .start()
    }

    // D-09: Semi-transparent overlay fades in on frozen frame
    private fun showProgressOverlay() {
        binding.progressOverlay.visibility = View.VISIBLE
        binding.progressOverlay.alpha = 0f
        binding.progressOverlay.animate()
            .alpha(1f)
            .setDuration(200L)
            .start()
    }

    private fun hideProgressOverlay() {
        binding.progressOverlay.visibility = View.GONE
    }

    private fun cropToCenterSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Make it a square based on the shorter side
        val size = Math.min(width, height)
        // Take 80% of the shortest side to really focus on the center
        // and eliminate the screen moiré/bezels at the edges
        val cropSize = (size * 0.8).toInt()

        val x = (width - cropSize) / 2
        val y = (height - cropSize) / 2

        return Bitmap.createBitmap(bitmap, x, y, cropSize, cropSize)
    }

    private fun runAskAiInference(customBitmap: Bitmap? = null) {
        if (isProcessing) return

        var bitmap = customBitmap
        if (bitmap == null) {
            val rawBitmap = latestBitmap
            if (rawBitmap == null) {
                Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
                return
            }
            // Center crop the camera preview to remove noisy background
            bitmap = cropToCenterSquare(rawBitmap)
        }

        // Check if model needs download first
        viewLifecycleOwner.lifecycleScope.launch {
            val isReady = modelDownloadManager.isModelReady()
            if (!isReady) {
                // Start download with WiFi check
                modelDownloadManager.downloadModel().collect { state ->
                    when (state) {
                        is DownloadUiState.WifiCheckRequired -> {
                            // Show WiFi warning dialog
                            showWifiWarningDialog(state.estimatedSizeMB, bitmap)
                        }
                        is DownloadUiState.Downloading -> {
                            isProcessing = true
                            showProgressOverlay()
                            binding.tvProgressTitle.text = "Downloading AI model..."
                            binding.tvProgressDetailOverlay.text = "Downloading model... ${state.overallProgress}%"
                            updateStatus("Downloading AI model...", Status.INITIALIZING)
                        }
                        is DownloadUiState.Complete -> {
                            isProcessing = false
                            hideProgressOverlay()
                            Toast.makeText(context, "Model downloaded! Starting analysis...", Toast.LENGTH_SHORT).show()
                            // Retry inference after download
                            runAskAiInference(bitmap)
                        }
                        is DownloadUiState.Error -> {
                            isProcessing = false
                            hideProgressOverlay()
                            Toast.makeText(context, "Download failed: ${state.message}", Toast.LENGTH_LONG).show()
                            updateStatus("Download failed", Status.ERROR)
                        }
                        else -> { /* ignore other states during download */ }
                    }
                }
                return@launch
            }

            // Model ready — run inference
            isProcessing = true
            showProgressOverlay()
            binding.tvProgressTitle.text = "Analyzing food..."
            binding.tvProgressDetailOverlay.text = "Preparing analysis"
            updateStatus("Analyzing food...", Status.ANALYZING)
            startProgressTicker()

            try {
                identifyFoodUseCase.invoke(bitmap).collect { result ->
                    isProcessing = false
                    stopProgressTicker()
                    hideProgressOverlay()

                    if (result.name == "Error") {
                        displayAiResult(result.name, null, result.rawResponse ?: "Error")
                        updateStatus("Analysis failed", Status.ERROR)
                    } else {
                        // Send result to InventoryFragment via FragmentResultListener
                        val bundle = android.os.Bundle().apply {
                            putString("food_name", result.name)
                            putString("food_name_zh", result.nameZh)
                            putString("expiry_date", result.expiryHint)
                            putFloat("confidence", result.confidence)
                        }
                        requireActivity().supportFragmentManager.setFragmentResult("llm_scan_result", bundle)

                        displayAiResult(result.name, result.nameZh, result.rawResponse ?: "")
                        updateStatus("Analysis complete", Status.READY)
                    }
                }
            } catch (e: Exception) {
                isProcessing = false
                stopProgressTicker()
                hideProgressOverlay()
                Log.e(TAG, "Inference error", e)
                Toast.makeText(context, "AI analysis failed: ${e.message}", Toast.LENGTH_LONG).show()
                updateStatus("Analysis failed", Status.ERROR)
            }
        }
    }

    private fun showWifiWarningDialog(estimatedSizeMB: Double, bitmap: Bitmap) {
        val message = "The AI model is approximately ${String.format("%.0f", estimatedSizeMB)}MB. " +
                     "Download over cellular may use significant data.\n\nDownload anyway?"

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Download Over Cellular")
            .setMessage(message)
            .setPositiveButton("Download") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    modelDownloadManager.downloadModel(skipWifiCheck = true).collect { state ->
                        when (state) {
                            is DownloadUiState.Downloading -> {
                                isProcessing = true
                                showProgressOverlay()
                                binding.tvProgressTitle.text = "Downloading AI model..."
                                binding.tvProgressDetailOverlay.text = "Downloading model... ${state.overallProgress}%"
                                updateStatus("Downloading AI model...", Status.INITIALIZING)
                            }
                            is DownloadUiState.Complete -> {
                                isProcessing = false
                                hideProgressOverlay()
                                Toast.makeText(context, "Model downloaded! Starting analysis...", Toast.LENGTH_SHORT).show()
                                runAskAiInference(bitmap)
                            }
                            is DownloadUiState.Error -> {
                                isProcessing = false
                                hideProgressOverlay()
                                Toast.makeText(context, "Download failed: ${state.message}", Toast.LENGTH_LONG).show()
                                updateStatus("Download failed", Status.ERROR)
                            }
                            else -> { /* ignore other states during download */ }
                        }
                    }
                }
            }
            .setNegativeButton("Wait for WiFi") { _, _ ->
                Toast.makeText(context, "Download will start when WiFi is available", Toast.LENGTH_SHORT).show()
            }
            .show()
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
                // D-10: Staged progress text
                val stage = when {
                    elapsedSec < 3 -> "Identifying food item..."
                    else -> "Almost done..."
                }
                _binding?.tvProgressDetailOverlay?.text = "$stage ${"%.1f".format(elapsedSec)}s elapsed"
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
        hideProgressOverlay()
        binding.btnCapture.isEnabled = true
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
        
        _binding = null
    }
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
