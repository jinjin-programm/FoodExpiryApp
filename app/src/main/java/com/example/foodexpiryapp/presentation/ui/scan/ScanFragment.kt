package com.example.foodexpiryapp.presentation.ui.scan

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
import com.example.foodexpiryapp.databinding.FragmentScanBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

@AndroidEntryPoint
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Manual capture state (D-12: frame capturer keeps latest bitmap)
    private var latestBitmap: Bitmap? = null
    private var isProcessing = false

    // Scan mode: "barcode" (default) or "date"
    private var scanMode = "barcode"

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
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check for initial scan mode from arguments
        val mode = arguments?.getString("scan_mode")
        if (mode == "date") {
            scanMode = "date"
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
    }

    private fun setupUI() {
        // D-04: Floating back arrow
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // D-12: Manual capture button
        binding.btnCapture.setOnClickListener {
            captureAndAnalyze()
        }

        // D-14: Confirm button in result card (barcode mode)
        binding.btnConfirmBarcode.setOnClickListener {
            val barcode = binding.tvBarcodeValue.text.toString()
            requireActivity().supportFragmentManager.setFragmentResult(
                "SCAN_RESULT", bundleOf("barcode" to barcode)
            )
            findNavController().popBackStack()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Camera setup
    // ────────────────────────────────────────────────────────────────────────

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
            .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, FrameCapturer()) }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Frame capture (keeps latest bitmap for shutter-triggered capture)
    // D-12: Manual capture — user taps capture, detection runs on captured frame
    // ────────────────────────────────────────────────────────────────────────

    private inner class FrameCapturer : ImageAnalysis.Analyzer {
        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val bitmap = imageProxy.toBitmap()
            val rotation = imageProxy.imageInfo.rotationDegrees
            if (bitmap != null && rotation != 0) {
                val matrix = android.graphics.Matrix()
                matrix.postRotate(rotation.toFloat())
                latestBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                latestBitmap = bitmap
            }
            imageProxy.close()
        }

        private fun ImageProxy.toBitmap(): Bitmap? {
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
    // Capture + flash + barcode/date detection flow
    // ────────────────────────────────────────────────────────────────────────

    private fun captureAndAnalyze() {
        if (isProcessing) return

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured — try again", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessing = true

        // D-08/D-11: Flash animation on capture
        showFlashAnimation {
            if (scanMode == "barcode") {
                detectBarcodeInBitmap(bitmap)
            } else {
                detectDateInBitmap(bitmap)
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Barcode detection on captured bitmap
    // ────────────────────────────────────────────────────────────────────────

    private fun detectBarcodeInBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && !barcodes[0].rawValue.isNullOrEmpty()) {
                    val barcodeValue = barcodes[0].rawValue!!
                    showBarcodeResult(barcodeValue)
                } else {
                    isProcessing = false
                    Toast.makeText(context, "No barcode detected — try again", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                isProcessing = false
                Toast.makeText(context, "Barcode scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Date detection on captured bitmap (manual capture mode)
    // ────────────────────────────────────────────────────────────────────────

    private fun detectDateInBitmap(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val datePattern = Pattern.compile(
            "\\b(\\d{1,2}[-/. ]\\d{1,2}[-/. ]\\d{2,4})|(\\d{4}[-/. ]\\d{1,2}[-/. ]\\d{1,2})\\b"
        )

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                val matcher = datePattern.matcher(text)
                if (matcher.find()) {
                    val potentialDate = matcher.group()
                    if (potentialDate != null) {
                        showBarcodeResult(potentialDate) // Reuse result card for date
                    } else {
                        isProcessing = false
                        Toast.makeText(context, "No date detected — try again", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isProcessing = false
                    Toast.makeText(context, "No date detected — try again", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                isProcessing = false
                Toast.makeText(context, "Date scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Result display
    // ────────────────────────────────────────────────────────────────────────

    /**
     * D-14: Show result in card with Confirm button.
     * For barcode mode, shows barcode value.
     * For date mode, shows detected date string.
     */
    private fun showBarcodeResult(value: String) {
        activity?.runOnUiThread {
            binding.tvBarcodeValue.text = value
            binding.cardBarcodeResult.visibility = View.VISIBLE
            isProcessing = false
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Flash animation (D-08: white flash on capture)
    // ────────────────────────────────────────────────────────────────────────

    private fun showFlashAnimation(onComplete: () -> Unit) {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 1f

        binding.flashOverlay.animate()
            .alpha(0f)
            .setDuration(150L)
            .withEndAction {
                binding.flashOverlay.visibility = View.GONE
                onComplete()
            }
            .start()
    }

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanFragment"
    }
}
