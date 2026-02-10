package com.example.foodexpiryapp.presentation.ui.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.foodexpiryapp.databinding.ActivityScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Camera activity for barcode scanning and OCR text recognition.
 * Returns the scanned data (barcode value or detected expiry date text) to the calling activity/fragment.
 */
class ScannerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SCAN_MODE = "scan_mode"
        const val EXTRA_BARCODE_RESULT = "barcode_result"
        const val EXTRA_OCR_TEXT_RESULT = "ocr_text_result"
        const val MODE_BARCODE = 0
        const val MODE_OCR = 1
        private const val TAG = "ScannerActivity"
    }

    private lateinit var binding: ActivityScannerBinding
    private lateinit var cameraExecutor: ExecutorService

    private var scanMode = MODE_BARCODE
    private var lastScannedBarcode: String? = null
    private var lastScannedText: String? = null
    private var isProcessing = false

    // Date patterns to look for in OCR
    private val datePatterns = listOf(
        Pattern.compile("(\\d{1,2})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{2,4})"),   // DD/MM/YYYY or MM/DD/YYYY
        Pattern.compile("(\\d{4})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{1,2})"),      // YYYY/MM/DD
        Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s*\\d{1,2},?\\s*\\d{4}"),
        Pattern.compile("(?i)\\d{1,2}\\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\\s*\\d{4}"),
        Pattern.compile("(?i)(best before|use by|exp|expires?)\\s*:?\\s*(.+)", Pattern.CASE_INSENSITIVE)
    )

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            binding.textScanResult.text = "Camera permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        scanMode = intent.getIntExtra(EXTRA_SCAN_MODE, MODE_BARCODE)

        setupUI()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupUI() {
        // Set initial chip selection
        if (scanMode == MODE_OCR) {
            binding.chipOcr.isChecked = true
        } else {
            binding.chipBarcode.isChecked = true
        }

        binding.scanModeGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            scanMode = if (checkedIds.contains(binding.chipOcr.id)) MODE_OCR else MODE_BARCODE
            lastScannedBarcode = null
            lastScannedText = null
            binding.btnUseResult.visibility = View.GONE
            binding.textScanResult.text = "Scanning..."
        }

        binding.btnUseResult.setOnClickListener {
            val resultIntent = Intent()
            if (scanMode == MODE_BARCODE && lastScannedBarcode != null) {
                resultIntent.putExtra(EXTRA_BARCODE_RESULT, lastScannedBarcode)
            } else if (lastScannedText != null) {
                resultIntent.putExtra(EXTRA_OCR_TEXT_RESULT, lastScannedText)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        binding.btnCloseScanner.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isProcessing = false
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        if (scanMode == MODE_BARCODE) {
            processBarcodes(inputImage, imageProxy)
        } else {
            processTextRecognition(inputImage, imageProxy)
        }
    }

    private fun processBarcodes(inputImage: InputImage, imageProxy: ImageProxy) {
        val scanner = BarcodeScanning.getClient()
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes.first()
                    val value = barcode.rawValue ?: barcode.displayValue ?: ""
                    if (value.isNotBlank()) {
                        lastScannedBarcode = value
                        runOnUiThread {
                            binding.textScanResult.text = "Barcode: $value"
                            binding.btnUseResult.visibility = View.VISIBLE
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scan failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing = false
            }
    }

    private fun processTextRecognition(inputImage: InputImage, imageProxy: ImageProxy) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                if (fullText.isNotBlank()) {
                    // Try to find date patterns
                    val detectedDate = findDateInText(fullText)
                    if (detectedDate != null) {
                        lastScannedText = detectedDate
                        runOnUiThread {
                            binding.textScanResult.text = "Detected date: $detectedDate"
                            binding.btnUseResult.visibility = View.VISIBLE
                        }
                    } else {
                        // Show raw text (truncated)
                        val preview = fullText.take(200)
                        runOnUiThread {
                            binding.textScanResult.text = "Text: $preview"
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing = false
            }
    }

    /**
     * Attempts to find a date string in OCR text.
     * Looks for common expiry-date patterns.
     */
    private fun findDateInText(text: String): String? {
        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group()
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
