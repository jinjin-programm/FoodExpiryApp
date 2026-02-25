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
        private const val DETECTION_INTERVAL_MS = 800L
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
        _binding = FragmentYoloScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialise detector off the main thread to avoid blocking the UI
        cameraExecutor.execute {
            yoloDetector = context?.let { YoloDetector(it) }
            activity?.runOnUiThread {
                val loaded = yoloDetector?.isModelLoaded() == true
                updateStatus(if (loaded) "YOLO26n ready – point at an object" else "Model unavailable")
            }
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

        binding.btnUseDetection.setOnClickListener {
            bestDetection?.let { detection ->
                returnDetectionResult(detection)
            }
        }

        binding.btnManualSelect.setOnClickListener {
            returnDetectionResult(
                DetectionResult(
                    label = "Food Item",
                    confidence = 0f,
                    boundingBox = android.graphics.RectF(),
                    category = FoodCategory.OTHER
                )
            )
        }

        binding.btnManualSelect.visibility = View.VISIBLE
    }

    // ────────────────────────────────────────────────────────────────────────
    // Camera
    // ────────────────────────────────────────────────────────────────────────

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            cameraProvider = future.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, FrameAnalyzer()) }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            updateStatus("Camera failed to start")
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Frame analysis
    // ────────────────────────────────────────────────────────────────────────

    private inner class FrameAnalyzer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val now = System.currentTimeMillis()

            if (isProcessing || now - lastDetectionTime < DETECTION_INTERVAL_MS) {
                imageProxy.close()
                return
            }

            if (imageProxy.image == null) {
                imageProxy.close()
                return
            }

            isProcessing = true

            try {
                val bitmap = imageProxy.toNv21Bitmap()
                if (bitmap != null) {
                    val detections = yoloDetector?.detect(bitmap) ?: emptyList()
                    activity?.runOnUiThread { handleDetections(detections, bitmap) }
                } else {
                    Log.w(TAG, "Frame conversion failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Frame analysis error", e)
            } finally {
                isProcessing = false
                lastDetectionTime = System.currentTimeMillis()
                imageProxy.close()
            }
        }

        /** Convert ImageProxy (YUV_420_888) → NV21 → JPEG → Bitmap. */
        private fun ImageProxy.toNv21Bitmap(): Bitmap? {
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
                yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
                BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
            } catch (e: Exception) {
                Log.e(TAG, "YUV→Bitmap conversion failed", e)
                null
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // UI updates
    // ────────────────────────────────────────────────────────────────────────

    private fun handleDetections(detections: List<DetectionResult>, bitmap: Bitmap) {
        if (detections.isNotEmpty()) {
            val best = detections.maxByOrNull { it.confidence }!!

            // Only surface detections above 50 % confidence in the action bar
            if (best.confidence >= 0.50f) {
                bestDetection = best
                binding.tvDetectedItem.text = "Detected: ${best.label.capitalizeWords()}"
                binding.tvConfidence.text = "Confidence: ${(best.confidence * 100).toInt()}%"
                updateStatus("${detections.size} object(s) detected")
                binding.detectionResultsContainer.visibility = View.VISIBLE
                binding.btnManualSelect.visibility = View.GONE
            } else {
                // Low-confidence results: show in status bar only, don't promote to action
                updateStatus("Low confidence – keep scanning (${(best.confidence * 100).toInt()}%)")
            }

            binding.detectionOverlay.setDetections(detections, bitmap.width, bitmap.height)
        } else {
            updateStatus("Scanning… point camera at an object")
            binding.detectionOverlay.clearDetections()
        }
    }

    private fun updateStatus(message: String) {
        binding.tvDetectionStatus.text = message
    }

    // ────────────────────────────────────────────────────────────────────────
    // Result
    // ────────────────────────────────────────────────────────────────────────

    private fun returnDetectionResult(detection: DetectionResult) {
        setFragmentResult(
            "YOLO_SCAN_RESULT",
            bundleOf(
                "yolo_label"      to detection.label,
                "yolo_category"   to detection.category.name,
                "yolo_confidence" to detection.confidence
            )
        )
        findNavController().popBackStack()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        yoloDetector?.close()
        _binding = null
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
