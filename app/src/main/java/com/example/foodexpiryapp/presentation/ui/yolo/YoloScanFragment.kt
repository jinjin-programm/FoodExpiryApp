package com.example.foodexpiryapp.presentation.ui.yolo

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
import com.google.android.material.snackbar.Snackbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.foodexpiryapp.databinding.FragmentYoloScanBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class YoloScanFragment : Fragment() {

    companion object {
        private const val TAG = "YoloScanFragment"
        private const val DETECTION_INTERVAL = 1000L // ms between detections
    }

    private var _binding: FragmentYoloScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var yoloDetector: YoloDetector? = null

    private var isProcessing = false
    private var lastDetectionTime = 0L
    private var bestDetection: DetectionResult? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
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
        _binding = FragmentYoloScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        yoloDetector = context?.let { YoloDetector(it) }

        showSnackbar("YOLO model loading...")
        
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

        binding.btnUseDetection.setOnClickListener {
            bestDetection?.let { detection ->
                showSnackbar("Adding ${detection.label} to inventory (${(detection.confidence * 100).toInt()}% confidence)")
                returnDetectionResult(detection)
            }
        }

        binding.btnManualSelect.setOnClickListener {
            // Return a generic food item for manual editing
            returnDetectionResult(
                DetectionResult(
                    label = "Food Item",
                    confidence = 0f,
                    boundingBox = android.graphics.RectF(),
                    category = FoodCategory.OTHER
                )
            )
        }

        // Show manual button initially
        binding.btnManualSelect.visibility = View.VISIBLE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, ObjectAnalyzer())
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            showSnackbar("Camera started - YOLO ready")
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
            showSnackbar("Camera failed to start")
        }
    }

    private inner class ObjectAnalyzer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val currentTime = System.currentTimeMillis()
            
            if (isProcessing || currentTime - lastDetectionTime < DETECTION_INTERVAL) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            isProcessing = true

            try {
                // Convert YUV image to bitmap
                val bitmap = imageProxy.convertToBitmap()
                
                if (bitmap != null) {
                    activity?.runOnUiThread {
                        showSnackbar("Running YOLO detection...")
                    }
                    
                    val detections = yoloDetector?.detect(bitmap) ?: emptyList()
                    
                    activity?.runOnUiThread {
                        if (detections.isNotEmpty()) {
                            val best = detections.maxByOrNull { it.confidence }
                            showSnackbar("YOLO detected ${detections.size} objects. Best: ${best?.label} (${(best?.confidence?.times(100))?.toInt()}%)")
                        } else {
                            showSnackbar("YOLO: No objects detected")
                        }
                        processDetections(detections, bitmap)
                    }
                } else {
                    activity?.runOnUiThread {
                        showSnackbar("YOLO: Failed to convert image")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image", e)
                activity?.runOnUiThread {
                    showSnackbar("YOLO error: ${e.message}")
                }
            } finally {
                isProcessing = false
                lastDetectionTime = System.currentTimeMillis()
                imageProxy.close()
            }
        }

        private fun ImageProxy.convertToBitmap(): Bitmap? {
            return try {
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
                
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                Log.e(TAG, "Error converting YUV to bitmap", e)
                null
            }
        }
    }

    private fun processDetections(detections: List<DetectionResult>, bitmap: Bitmap) {
        if (detections.isNotEmpty()) {
            // Find the best detection (highest confidence)
            val best = detections.maxByOrNull { it.confidence }
            
            if (best != null && best.confidence > 0.5f) {
                bestDetection = best
                
                // Update UI
                binding.tvDetectedItem.text = "Detected: ${best.label.capitalizeWords()}"
                binding.tvConfidence.text = "Confidence: ${(best.confidence * 100).toInt()}%"
                binding.tvDetectionStatus.text = "Food item detected!"
                
                // Show results container
                binding.detectionResultsContainer.visibility = View.VISIBLE
                binding.btnManualSelect.visibility = View.GONE
                
                // Update overlay with bounding boxes
                binding.detectionOverlay.setDetections(detections, bitmap.width, bitmap.height)
            }
        } else {
            // No detections
            binding.tvDetectionStatus.text = "Scanning... Point camera at food items"
            binding.detectionOverlay.clearDetections()
        }
    }

    private fun returnDetectionResult(detection: DetectionResult) {
        val resultBundle = bundleOf(
            "yolo_label" to detection.label,
            "yolo_category" to detection.category.name,
            "yolo_confidence" to detection.confidence
        )
        
        setFragmentResult("YOLO_SCAN_RESULT", resultBundle)
        findNavController().popBackStack()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
        Log.d(TAG, "Snackbar: $message")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        yoloDetector?.close()
        _binding = null
    }
}

// Extension function to capitalize first letter of each word
private fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
