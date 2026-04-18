package com.example.foodexpiryapp.presentation.ui.yolo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.example.foodexpiryapp.BuildConfig
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentYoloScanBinding
import com.example.foodexpiryapp.presentation.ui.scan.ScanPagerAdapter
import com.example.foodexpiryapp.presentation.viewmodel.YoloScanViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * YOLO scan tab with camera preview + bounding box overlay + staged progress.
 *
 * Replaces the previous TFLite YoloDetector with the MNN pipeline via
 * YoloScanViewModel → YoloDetectionRepository.
 *
 * Per D-02: Bounding boxes on camera preview via DetectionOverlayView.
 * Per D-07: Staged progress — Detecting → Identifying(n/total) → Navigate to Confirmation.
 * Per D-19: 0 items detected shows helpful message with retry option.
 * Per D-21: Model lifecycle during tab switch — cancel and unload when swiping away.
 */
@AndroidEntryPoint
class YoloScanFragment : Fragment() {

    companion object {
        private const val TAG = "YoloScanFragment"
    }

    private var _binding: FragmentYoloScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: YoloScanViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var latestBitmap: Bitmap? = null
    private var isCapturing = false

    // Per D-06: Overlay for bounding box rendering
    private lateinit var detectionOverlay: DetectionOverlayView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(context, "Camera permission required for scanning", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handleGalleryImage(uri)
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

        // Per D-06: Initialize detection overlay for bounding box rendering
        detectionOverlay = binding.detectionOverlay

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
        observeUiState()
        observeDetections()
        setupViewPagerCallback()
        setupSaveResultListener()
        loadLatestGalleryThumbnail()
    }

    // ────────────────────────────────────────────────────────────────────────
    // UI setup
    // ────────────────────────────────────────────────────────────────────────

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCapture.setOnClickListener {
            captureAndDetect()
        }

        binding.galleryContainer.setOnClickListener {
            if (!isCapturing) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        showProgressOverlay(false)
    }

    /**
     * Observe ViewModel UI state and update the progress overlay.
     * Per D-07: Staged progress — Detecting → Identifying(n/total) → Complete.
     */
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is YoloScanViewModel.YoloScanUiState.Ready -> {
                            showProgressOverlay(false)
                            isCapturing = false
                        }
                        is YoloScanViewModel.YoloScanUiState.Detecting -> {
                            // Stage 1: "Detecting food items..."
                            showProgressOverlay(true)
                            binding.tvProgressTitle.text = "Detecting food items..."
                            binding.tvProgressDetail.text = "Analyzing camera frame"
                            binding.btnCancel.visibility = View.GONE
                        }
                        is YoloScanViewModel.YoloScanUiState.Classifying -> {
                            // Stage 2: "Identifying item (1/5)..."
                            binding.tvProgressTitle.text = "Identifying item (${state.current}/${state.total})..."
                            binding.tvProgressDetail.text = "AI classification in progress"
                            binding.btnCancel.visibility = View.VISIBLE
                        }
                        is YoloScanViewModel.YoloScanUiState.Complete -> {
                            // Stage 3: Auto-navigate to ConfirmationFragment
                            showProgressOverlay(false)
                            isCapturing = false
                            navigateToConfirmation(state.sessionId)
                        }
                        is YoloScanViewModel.YoloScanUiState.Error -> {
                            showProgressOverlay(false)
                            isCapturing = false
                            Toast.makeText(context, "Detection failed: ${state.message}", Toast.LENGTH_LONG).show()
                        }
                        is YoloScanViewModel.YoloScanUiState.NoDetection -> {
                            // D-19: 0 items detected
                            showNoDetectionMessage()
                            isCapturing = false
                        }
                    }
                }
            }
        }
    }

    /**
     * Per D-06: Observe detections for overlay rendering.
     * Updates bounding boxes as YOLO detects items and LLM classifies them.
     */
    private fun observeDetections() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detections.collect { detections ->
                    if (detections.isNotEmpty() && latestBitmap != null) {
                        if (BuildConfig.DEBUG) {
                            for ((idx, det) in detections.withIndex()) {
                                Log.d("OnnxYoloEngine-Debug", "Overlay detection $idx: label=${det.label}, conf=${String.format("%.2f", det.confidence)}, bbox=${det.boundingBox}")
                            }
                        }
                        detectionOverlay.setDetections(
                            detections,
                            latestBitmap!!.width,
                            latestBitmap!!.height
                        )
                    }
                }
            }
        }
    }

    /**
     * Per D-21: Cancel pending analysis and unload YOLO model when swiping away.
     * Re-bind camera when swiping back to YOLO tab — other fragments call
     * ProcessCameraProvider.unbindAll() which removes our use cases.
     */
    private fun setupViewPagerCallback() {
        val viewPager = requireParentFragment().view?.findViewById<ViewPager2>(R.id.viewPager) ?: return
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == ScanPagerAdapter.TAB_YOLO) {
                    // Re-bind camera use cases — another fragment's unbindAll() may
                    // have removed our preview / analyzer from the shared ProcessCameraProvider.
                    bindCameraUseCases()
                } else {
                    // Cancel any ongoing pipeline when leaving YOLO tab
                    viewModel.cancelDetection()
                    showProgressOverlay(false)
                    isCapturing = false
                }
            }
        })
    }

    /**
     * Per D-15: Listens for save completion from ConfirmationFragment.
     * Shows Snackbar with "View" action that navigates to inventory with highlight flag.
     * Per D-18: Camera preview is already running since we popped back to this fragment.
     */
    private fun setupSaveResultListener() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "yolo_save_complete", viewLifecycleOwner
        ) { _, bundle ->
            val savedCount = bundle.getInt("saved_count", 0)
            // Per D-15: Snackbar with "View" action
            Snackbar.make(binding.root, "$savedCount item(s) added to fridge", Snackbar.LENGTH_LONG)
                .setAction("View") {
                    // Navigate to inventory with highlight flag
                    val invBundle = bundleOf("highlightNew" to true)
                    requireActivity().supportFragmentManager.setFragmentResult("highlight_new_items", invBundle)
                    findNavController().popBackStack()
                }
                .show()

            // Per D-18: Camera preview resumes automatically since we're back on this fragment
        }
    }

    private fun showProgressOverlay(show: Boolean) {
        if (show) {
            showProgressOverlayWithFadeIn()
        } else {
            binding.progressOverlay.visibility = View.GONE
        }
        binding.btnCapture.isEnabled = !show
    }

    // D-09: Semi-transparent overlay fades in on frozen frame
    private fun showProgressOverlayWithFadeIn() {
        binding.progressOverlay.visibility = View.VISIBLE
        binding.progressOverlay.alpha = 0f
        binding.progressOverlay.animate()
            .alpha(1f)
            .setDuration(200L)
            .start()
    }

    /**
     * Per D-19: Show "No food items detected" with retry and switch-to-Photo-Scan options.
     */
    private fun showNoDetectionMessage() {
        showProgressOverlay(true)
        binding.tvProgressTitle.text = "No food items detected"
        binding.tvProgressDetail.text = "Try adjusting the camera or lighting"
        binding.btnCancel.text = "Retry"
        binding.btnCancel.visibility = View.VISIBLE
        binding.btnCancel.setOnClickListener {
            viewModel.resetToReady()
            showProgressOverlay(false)
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Gallery picker
    // ────────────────────────────────────────────────────────────────────────

    private fun loadLatestGalleryThumbnail() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resolver = requireContext().contentResolver
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sortOrder
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val id = cursor.getLong(idColumn)
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        withContext(Dispatchers.Main) {
                            _binding?.imgRecentScan?.setImageURI(uri)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun handleGalleryImage(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resolver = requireContext().contentResolver
                val inputStream: InputStream? = resolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                val targetSize = 1024
                val sampleSize = maxOf(
                    options.outWidth / targetSize,
                    options.outHeight / targetSize
                ).coerceAtLeast(1)

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val secondStream = resolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                secondStream?.close()

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to decode image", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val safeBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false) ?: bitmap

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    isCapturing = true
                    showProgressOverlayWithFadeIn()
                    viewModel.startDetection(safeBitmap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gallery image load failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, FrameCapturer()) }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                viewLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }

    /**
     * Captures the latest bitmap and triggers the detection pipeline via ViewModel.
     * Per D-08: White flash animation before detection.
     */
    private fun captureAndDetect() {
        if (isCapturing) return

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured — try again", Toast.LENGTH_SHORT).show()
            return
        }

        isCapturing = true

        // Per D-11: Defensive copy — latestBitmap can be recycled by camera callback during inference
        val safeBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
            ?: run {
                isCapturing = false
                Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show()
                return
            }

        // D-08: White flash animation
        showFlashAnimation {
            // D-09: Show progress overlay over frozen frame
            showProgressOverlayWithFadeIn()
            viewModel.startDetection(safeBitmap)
        }
    }

    // D-08: White flash animation helper
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

    /**
     * Navigates to ConfirmationFragment with sessionId as argument.
     * Per D-16: Navigation Component for navigation from scan to confirmation.
     */
    private fun navigateToConfirmation(sessionId: String) {
        try {
            val bundle = Bundle().apply { putString("sessionId", sessionId) }
            findNavController().navigate(R.id.action_scan_container_to_confirmation, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to confirmation failed", e)
            // Fallback: set FragmentResult for compatibility
            isCapturing = false
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Frame capture (keeps latest bitmap for shutter-triggered capture)
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
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        viewModel.cancelDetection()
        if (::detectionOverlay.isInitialized) {
            detectionOverlay.clearDetections()
        }
        _binding = null
    }
}
