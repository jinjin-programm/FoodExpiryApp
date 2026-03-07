package com.example.foodexpiryapp.presentation.ui.vision

import android.Manifest
import android.content.Context
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
import com.example.foodexpiryapp.presentation.ui.llm.LlamaBridge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class VisionScanFragment : Fragment() {

    companion object {
        private const val TAG = "VisionScanFragment"
        private const val MODEL_DIR = "llm"
        private const val MODEL_FILE = "model.gguf"
        private const val MMPROJ_FILE = "mmproj.gguf"
        private const val FOOD_PROMPT = """Identify the food item and expiry date in this image. 

Respond in exactly this format with no other text:
FOOD: [food name]
EXPIRY: [date in DD/MM/YYYY format, or "not visible" if not shown]"""
    }

    private var _binding: FragmentVisionScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var latestBitmap: Bitmap? = null
    private var isProcessing = false
    private var detectionJob: Job? = null
    private var modelLoadJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val llamaBridge = LlamaBridge.getInstance()

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

        updateStatus("Loading Qwen3-VL...", Status.INITIALIZING)
        binding.progressBar.visibility = View.VISIBLE

        // Load model if not already loaded
        loadModelIfNeeded()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
    }

    private fun loadModelIfNeeded() {
        if (llamaBridge.isLoaded() && llamaBridge.hasVisionSupport()) {
            updateStatus("Qwen3-VL ready - tap capture", Status.READY)
            binding.progressBar.visibility = View.GONE
            return
        }

        modelLoadJob = scope.launch {
            updateStatus("Loading model...", Status.INITIALIZING)
            
            try {
                val success = withContext(Dispatchers.IO) {
                    loadModelInternal()
                }
                
                if (success && llamaBridge.hasVisionSupport()) {
                    updateStatus("Qwen3-VL ready - tap capture", Status.READY)
                } else if (success) {
                    updateStatus("Model loaded (text only)", Status.ERROR)
                    Toast.makeText(context, "Vision not available - mmproj missing", Toast.LENGTH_LONG).show()
                } else {
                    updateStatus("Failed to load model", Status.ERROR)
                    Toast.makeText(context, "Failed to load model", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model load error", e)
                updateStatus("Error: ${e.message}", Status.ERROR)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadModelInternal(): Boolean {
        val context = requireContext()
        val modelPath = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelPath, MODEL_FILE)
        val mmprojFile = File(modelPath, MMPROJ_FILE)

        // Copy model from assets if needed
        if (!modelFile.exists()) {
            try {
                context.assets.open("$MODEL_DIR/$MODEL_FILE").use { input ->
                    modelPath.mkdirs()
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Model copied from assets")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy model: ${e.message}")
                return false
            }
        }

        // Copy mmproj from assets if needed
        if (!mmprojFile.exists()) {
            try {
                context.assets.open("$MODEL_DIR/$MMPROJ_FILE").use { input ->
                    modelPath.mkdirs()
                    mmprojFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Mmproj copied from assets")
            } catch (e: Exception) {
                Log.w(TAG, "Mmproj not found: ${e.message}")
            }
        }

        // Load model
        if (!llamaBridge.isLoaded()) {
            Log.i(TAG, "Loading model from: ${modelFile.absolutePath}")
            // Use more threads for faster inference (adjust based on device)
            val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(4, 8)
            Log.i(TAG, "Using $numThreads threads for inference")
            // Use 4096 context for vision model (2048 is too small)
            val loaded = llamaBridge.loadModel(modelFile.absolutePath, 4096, numThreads)
            if (!loaded) {
                Log.e(TAG, "Failed to load model")
                return false
            }
        }

        // Load mmproj
        if (mmprojFile.exists() && !llamaBridge.hasVisionSupport()) {
            Log.i(TAG, "Loading mmproj from: ${mmprojFile.absolutePath}")
            llamaBridge.loadMmproj(mmprojFile.absolutePath)
        }

        return llamaBridge.isLoaded()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCapture.setOnClickListener {
            captureAndAnalyze()
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
                        latestBitmap = imageProxy.toBitmap()
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

        if (!llamaBridge.isLoaded()) {
            Toast.makeText(context, "Model not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        if (!llamaBridge.hasVisionSupport()) {
            Toast.makeText(context, "Vision not available", Toast.LENGTH_SHORT).show()
            return
        }

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return
        }

        // Resize image for faster processing (320x240)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 240, true)
        Log.d(TAG, "Image resized from ${bitmap.width}x${bitmap.height} to ${resizedBitmap.width}x${resizedBitmap.height}")

        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false
        binding.tvProgressDetail.visibility = View.VISIBLE
        binding.tvInstruction.visibility = View.GONE
        updateProgress("Step 1/3: Encoding image...")
        updateStatus("Analyzing with Qwen3-VL...", Status.ANALYZING)

        detectionJob = scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // Update progress before native call
                withContext(Dispatchers.Main) {
                    updateProgress("Step 2/3: Processing with AI...")
                }
                
                val response = withContext(Dispatchers.IO) {
                    llamaBridge.generateWithImage(FOOD_PROMPT, resizedBitmap)
                }
                
                withContext(Dispatchers.Main) {
                    updateProgress("Step 3/3: Parsing result...")
                }
                
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                Log.d(TAG, "Vision response (${elapsed}s): $response")
                
                val result = parseFoodResponse(response)
                
                displayResult(result.foodName, result.expiryDate, result.rawResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error: ${e.message}", e)
                updateStatus("Error: ${e.message}", Status.ERROR)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
                binding.progressBar.visibility = View.GONE
                binding.tvProgressDetail.visibility = View.GONE
                binding.tvInstruction.visibility = View.VISIBLE
                binding.btnCapture.isEnabled = true
            }
        }
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
        // Regex to find FOOD: [value] and EXPIRY: [value] patterns
        // Handles optional brackets and matches last occurrence (most reliable)
        val foodRegex = Regex("""FOOD:\s*\[?([^\[\]\n]+)\]?"?""", RegexOption.IGNORE_CASE)
        val expiryRegex = Regex("""EXPIRY:\s*\[?([^\[\]\n]+)\]?"?""", RegexOption.IGNORE_CASE)
        
        // Find all matches and take the LAST one (model often repeats correct answer at end)
        val foodMatches = foodRegex.findAll(response).toList()
        val expiryMatches = expiryRegex.findAll(response).toList()
        
        var foodName = foodMatches.lastOrNull()?.groupValues?.get(1)?.trim() ?: "Unknown"
        val expiryDate = expiryMatches.lastOrNull()?.groupValues?.get(1)?.trim()
        
        // Clean up "not visible" cases
        val cleanExpiry = expiryDate?.takeIf { 
            !it.equals("not visible", ignoreCase = true) && 
            !it.equals("not shown", ignoreCase = true) &&
            it.isNotBlank() 
        }
        
        // Fallback if no food found
        if (foodName == "Unknown" || foodName.isBlank()) {
            foodName = response.take(50).ifEmpty { "Unknown" }
        }
        
        return FoodResult(foodName, cleanExpiry, response)
    }

    private fun displayResult(foodName: String, expiryDate: String?, rawResponse: String) {
        binding.tvFoodName.text = foodName
        binding.tvExpiryDate.text = "Expiry: ${expiryDate ?: "Not detected"}"
        binding.tvRawResponse.text = rawResponse
        
        binding.resultCard.visibility = View.VISIBLE
        binding.rawResponseCard.visibility = View.VISIBLE
        updateStatus("Detection complete", Status.READY)

        binding.resultCard.postDelayed({
            _binding?.resultCard?.visibility = View.GONE
            _binding?.rawResponseCard?.visibility = View.GONE
        }, 15000)
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
        scope.cancel()
        cameraExecutor.shutdown()
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
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}