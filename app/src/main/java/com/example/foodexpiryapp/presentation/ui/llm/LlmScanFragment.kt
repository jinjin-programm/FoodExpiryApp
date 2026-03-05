package com.example.foodexpiryapp.presentation.ui.llm

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentLlmScanBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class LlmScanFragment : Fragment() {

    companion object {
        private const val TAG = "LlmScanFragment"
        private const val DETECTION_INTERVAL_MS = 3000L
    }

    private var _binding: FragmentLlmScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var llmVisionService: LlmVisionService? = null

    private var isProcessing = false
    private var lastDetectionTime = 0L
    private var latestBitmap: Bitmap? = null
    private var detectionJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(context, "Camera permission required for scanning", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLlmScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize LLM service using coroutine
        llmVisionService = LlmVisionService(requireContext())
        scope.launch {
            val initSuccess = llmVisionService?.initialize() ?: false
            updateStatus(if (initSuccess) "Qwen3.5 ready - tap capture to scan" else "LLM init failed - tap capture to retry")
            binding.progressBar.visibility = View.GONE
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
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

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
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

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(imageProxy: ImageProxy) {
        // Keep latest frame for capture
        latestBitmap = imageProxy.toBitmap()
        imageProxy.close()
    }

    private fun captureAndAnalyze() {
        if (isProcessing) {
            return
        }

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured yet", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCapture.isEnabled = false
        updateStatus("Analyzing image with Qwen3.5...")

        detectionJob = scope.launch {
            try {
                val result = llmVisionService?.analyzeImage(bitmap)
                
                result?.let {
                    displayResult(it.foodName, it.expiryDate, it.confidence)
                    
                    // Show detailed response in toast for debugging
                    if (it.foodName.contains("not found") || it.foodName.contains("failed")) {
                        Toast.makeText(context, it.rawResponse, Toast.LENGTH_LONG).show()
                    }
                    
                    // Pass result back to inventory fragment
                    setFragmentResult(
                        "llm_scan_result",
                        bundleOf(
                            "food_name" to it.foodName,
                            "expiry_date" to (it.expiryDate ?: ""),
                            "confidence" to it.confidence,
                            "detection_type" to "llm"
                        )
                    )
                } ?: run {
                    Toast.makeText(context, "Detection failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
                binding.progressBar.visibility = View.GONE
                binding.btnCapture.isEnabled = true
            }
        }
    }

    private fun displayResult(foodName: String, expiryDate: String?, confidence: String) {
        binding.tvFoodName.text = foodName
        binding.tvExpiryDate.text = expiryDate ?: "Not detected"
        binding.tvConfidence.text = "Confidence: $confidence"
        
        binding.resultCard.visibility = View.VISIBLE
        updateStatus("Detection complete")

        // Auto-dismiss result after 3 seconds
        binding.resultCard.postDelayed({
            binding.resultCard.visibility = View.GONE
        }, 5000)
    }

    private fun updateStatus(message: String) {
        binding.tvStatus.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detectionJob?.cancel()
        scope.cancel()
        cameraExecutor.shutdown()
        llmVisionService?.release()
        _binding = null
    }
}
