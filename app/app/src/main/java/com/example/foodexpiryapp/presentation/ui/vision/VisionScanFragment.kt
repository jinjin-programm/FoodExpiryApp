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
import com.example.foodexpiryapp.presentation.ui.llm.LlamaBridge
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
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class VisionScanFragment : Fragment() {

    companion object {
        private const val TAG = "VisionScanFragment"
        private const val MODEL_DIR = "llm"
        private const val MODEL_FILE = "model.gguf"
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

        updateStatus("Loading Qwen3.5-0.8B...", Status.INITIALIZING)
        binding.progressBar.visibility = View.VISIBLE
        binding.tvProgressDetail.text = "Loading text model..."

        loadModelIfNeeded()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
    }

    private fun loadModelIfNeeded() {
        if (llamaBridge.isLoaded()) {
            updateStatus("Model ready - tap capture", Status.READY)
            binding.progressBar.visibility = View.GONE
            binding.tvProgressDetail.text = "Point at food item and tap capture"
            return
        }

        modelLoadJob = scope.launch {
            updateStatus("Loading model...", Status.INITIALIZING)
            updateProgress("Copying model files...")

            try {
                val success = withContext(Dispatchers.IO) {
                    loadModelInternal()
                }

                if (success) {
                    updateStatus("Vision model ready - tap capture", Status.READY)
                    updateProgress("Ready")
                } else {
                    updateStatus("Failed to load model", Status.ERROR)
                    Toast.makeText(context, "Failed to load model", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Model load error", e)
                updateStatus("Error: ${e.message}", Status.ERROR)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.tvProgressDetail.visibility = View.GONE
                binding.tvInstruction.visibility = View.VISIBLE
            }
        }
    }

    private fun loadModelInternal(): Boolean {
        val context = requireContext()
        val modelPath = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelPath, MODEL_FILE)
        val mmprojFile = File(modelPath, "mmproj.gguf")

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
                context.assets.open("$MODEL_DIR/mmproj.gguf").use { input ->
                    modelPath.mkdirs()
                    mmprojFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Mmproj copied from assets")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy mmproj: ${e.message}")
            }
        }

        // Load text-only model (Qwen3.5-0.8B)
        if (!llamaBridge.isLoaded()) {
            Log.i(TAG, "Loading model from: ${modelFile.absolutePath}")
            
            // Optimal settings for 0.8B model on mobile
            val numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)
            Log.i(TAG, "Using $numThreads threads for inference")
            
            val loaded = llamaBridge.loadModel(modelFile.absolutePath, 2048, numThreads)
            if (!loaded) {
                Log.e(TAG, "Failed to load model")
                return false
            }
            
            if (mmprojFile.exists() && loaded) {
                Log.i(TAG, "Loading vision encoder from: ${mmprojFile.absolutePath}")
                llamaBridge.loadMmproj(mmprojFile.absolutePath)
            }
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

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false
        binding.tvProgressDetail.visibility = View.VISIBLE
        binding.tvInstruction.visibility = View.GONE
        updateProgress("Analyzing image with Qwen vision...")
        updateStatus("Thinking...", Status.ANALYZING)

        detectionJob = scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                val prompt = "Identify the food item and its expiry date. Answer ONLY in this format:\nFOOD: [name]\nEXPIRY: [date or \"not visible\"]"
                
                val response = withContext(Dispatchers.IO) {
                    llamaBridge.generateWithImage(prompt, bitmap, maxTokens = 150)
                }
                
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                Log.d(TAG, "Total time: ${elapsed}s, Response: $response")
                
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
        val foodRegex = Regex("""FOOD:\s*\[?([^\[\]\n]+)\]?"?""", RegexOption.IGNORE_CASE)
        val expiryRegex = Regex("""EXPIRY:\s*\[?([^\[\]\n]+)\]?"?""", RegexOption.IGNORE_CASE)
        
        val foodMatches = foodRegex.findAll(response).toList()
        val expiryMatches = expiryRegex.findAll(response).toList()
        
        var foodName = foodMatches.lastOrNull()?.groupValues?.get(1)?.trim() ?: "Unknown"
        val expiryDate = expiryMatches.lastOrNull()?.groupValues?.get(1)?.trim()
        
        val cleanExpiry = expiryDate?.takeIf { 
            !it.equals("not visible", ignoreCase = true) && 
            !it.equals("not shown", ignoreCase = true) &&
            it.isNotBlank() 
        }
        
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

        // Don't hide automatically so user can scroll and read long output
        /*
        binding.resultCard.postDelayed({
            _binding?.resultCard?.visibility = View.GONE
            _binding?.rawResponseCard?.visibility = View.GONE
        }, 15000)
        */
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