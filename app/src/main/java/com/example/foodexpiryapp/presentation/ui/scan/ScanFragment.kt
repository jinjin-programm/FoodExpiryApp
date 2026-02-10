package com.example.foodexpiryapp.presentation.ui.scan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.AndroidEntryPoint
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

    private var isScanningBarcode = true // Default mode
    private var isProcessing = false // Prevent multiple rapid scans

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

        cameraExecutor = Executors.newSingleThreadExecutor()

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

        binding.btnScanBarcode.setOnClickListener {
            isScanningBarcode = true
            updateModeUI()
            restartCamera()
        }

        binding.btnScanDate.setOnClickListener {
            isScanningBarcode = false
            updateModeUI()
            restartCamera()
        }
        
        updateModeUI()
    }

    private fun updateModeUI() {
        if (isScanningBarcode) {
            binding.tvInstruction.text = "Point camera at barcode"
            binding.btnScanBarcode.isEnabled = false
            binding.btnScanDate.isEnabled = true
        } else {
            binding.tvInstruction.text = "Point camera at expiry date (e.g., EXP 12/2025)"
            binding.btnScanBarcode.isEnabled = true
            binding.btnScanDate.isEnabled = false
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun restartCamera() {
        cameraProvider?.unbindAll()
        bindCameraUseCases()
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
                it.setAnalyzer(cameraExecutor, if (isScanningBarcode) BarcodeAnalyzer() else TextAnalyzer())
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null && !isProcessing) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (!barcode.rawValue.isNullOrEmpty()) {
                                isProcessing = true
                                handleBarcodeResult(barcode.rawValue!!)
                                break 
                            }
                        }
                    }
                    .addOnFailureListener {
                        // Task failed with an exception
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private inner class TextAnalyzer : ImageAnalysis.Analyzer {
        private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        // Simple regex for dates like DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD
        // This is a basic implementation and can be improved
        private val datePattern = Pattern.compile(
            "\\b(\\d{1,2}[-/. ]\\d{1,2}[-/. ]\\d{2,4})|(\\d{4}[-/. ]\\d{1,2}[-/. ]\\d{1,2})\\b"
        )

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null && !isProcessing) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        val matcher = datePattern.matcher(text)
                        if (matcher.find()) {
                            val potentialDate = matcher.group()
                            isProcessing = true
                            handleDateResult(potentialDate)
                        }
                    }
                    .addOnFailureListener {
                        // Task failed with an exception
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private fun handleBarcodeResult(barcode: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, "Barcode found: $barcode", Toast.LENGTH_SHORT).show()
            setFragmentResult("SCAN_RESULT", bundleOf("barcode" to barcode))
            findNavController().popBackStack()
        }
    }

    private fun handleDateResult(dateString: String) {
        activity?.runOnUiThread {
            // Try to parse the date to ensure it's valid, or just return the string
            // Here we just return the raw string and let the caller handle parsing or confirmation
            Toast.makeText(context, "Date found: $dateString", Toast.LENGTH_SHORT).show()
            setFragmentResult("SCAN_RESULT", bundleOf("date" to dateString))
            findNavController().popBackStack()
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "ScanFragment"
    }
}
