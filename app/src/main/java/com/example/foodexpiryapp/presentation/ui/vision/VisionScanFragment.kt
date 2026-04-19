package com.example.foodexpiryapp.presentation.ui.vision

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.example.foodexpiryapp.BuildConfig
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.data.remote.ollama.OllamaServerConfig
import com.example.foodexpiryapp.data.repository.DetectionResultRepository
import com.example.foodexpiryapp.data.repository.LlmInferenceRepositoryImpl
import com.example.foodexpiryapp.databinding.FragmentVisionScanBinding
import com.example.foodexpiryapp.domain.model.DetectionStatus
import com.example.foodexpiryapp.domain.model.PipelineState
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import com.example.foodexpiryapp.domain.usecase.IdentifyFoodUseCase
import com.example.foodexpiryapp.inference.pipeline.DetectionPipeline
import com.example.foodexpiryapp.presentation.ui.scan.ScanPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

@AndroidEntryPoint
class VisionScanFragment : Fragment() {

    companion object {
        private const val TAG = "VisionScanFragment"
        private const val PROGRESS_TICK_MS = 400L
    }

    private var _binding: FragmentVisionScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private var latestBitmap: Bitmap? = null
    private var isProcessing = false
    private var isCameraActive = true
    private var detectionJob: Job? = null
    private var progressTickerJob: Job? = null
    private var blurredBg: ImageView? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @Inject lateinit var identifyFoodUseCase: IdentifyFoodUseCase
    @Inject lateinit var llmRepository: LlmInferenceRepository
    @Inject lateinit var serverConfig: OllamaServerConfig
    @Inject lateinit var detectionPipeline: DetectionPipeline
    @Inject lateinit var detectionResultRepository: DetectionResultRepository

    private var isMultiMode = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
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
        _binding = FragmentVisionScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        updateStatus("Connecting to AI server...", Status.INITIALIZING)
        checkServerConnection()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupUI()
        setupViewPagerCallback()
        setupSaveResultListener()
        loadLatestGalleryThumbnail()
    }

    private fun checkServerConnection() {
        viewLifecycleOwner.lifecycleScope.launch {
            val repoImpl = llmRepository as? LlmInferenceRepositoryImpl
            if (repoImpl != null) {
                val isConnected = repoImpl.checkServerConnection()
                if (isConnected) {
                    updateStatus("AI server connected", Status.READY)
                } else {
                    updateStatus("AI server not connected — tap settings to configure", Status.ERROR)
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSettings.setOnClickListener {
            val dialog = OllamaSettingsDialog()
            dialog.show(parentFragmentManager, "ollama_settings")
            parentFragmentManager.setFragmentResultListener("settings_saved", viewLifecycleOwner) { _, _ ->
                checkServerConnection()
            }
        }

        binding.btnCapture.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            captureAndAnalyze()
        }

        binding.galleryThumbnail.setOnClickListener {
            if (isProcessing) return@setOnClickListener
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnRetake.setOnClickListener {
            binding.resultCard.visibility = View.GONE
            binding.rawResponseCard.visibility = View.GONE
            hideBlurredBackground()
            binding.flashOverlay.visibility = View.GONE
            restartCamera()
        }

        binding.btnCancelProgress.setOnClickListener {
            cancelOngoingInference()
        }

        binding.btnCancelBottom.setOnClickListener {
            cancelOngoingInference()
        }

        binding.btnAskAi.setOnClickListener {
            runOllamaAnalysis()
        }

        binding.btnSingleMode.setOnClickListener { setScanMode(false) }
        binding.btnMultiMode.setOnClickListener { setScanMode(true) }
        updateScanModeUI()
    }

    private fun setScanMode(multi: Boolean) {
        isMultiMode = multi
        updateScanModeUI()
    }

    private fun updateScanModeUI() {
        val activeBg = 0xE65100.toInt()
        val inactiveBg = android.graphics.Color.TRANSPARENT
        val activeColor = android.graphics.Color.WHITE
        val inactiveColor = 0xB3FFFFFF.toInt()

        if (isMultiMode) {
            binding.btnMultiMode.setBackgroundColor(activeBg)
            binding.btnMultiMode.setTextColor(activeColor)
            binding.btnMultiMode.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.btnSingleMode.setBackgroundColor(inactiveBg)
            binding.btnSingleMode.setTextColor(inactiveColor)
            binding.btnSingleMode.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.focusRectangle.visibility = View.GONE
        } else {
            binding.btnSingleMode.setBackgroundColor(activeBg)
            binding.btnSingleMode.setTextColor(activeColor)
            binding.btnSingleMode.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.btnMultiMode.setBackgroundColor(inactiveBg)
            binding.btnMultiMode.setTextColor(inactiveColor)
            binding.btnMultiMode.setTypeface(null, android.graphics.Typeface.NORMAL)
            binding.focusRectangle.visibility = View.VISIBLE
        }
    }

    private var viewPagerCallback: ViewPager2.OnPageChangeCallback? = null

    private fun setupViewPagerCallback() {
        val viewPager = try {
            requireParentFragment().view?.findViewById<ViewPager2>(R.id.viewPager)
        } catch (_: Exception) {
            null
        } ?: return
        viewPagerCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == ScanPagerAdapter.TAB_PHOTO && !isProcessing && _binding != null) {
                    if (!isCameraActive) {
                        startCamera()
                    }
                }
            }
        }
        viewPager.registerOnPageChangeCallback(viewPagerCallback!!)
    }

    private fun setupSaveResultListener() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "yolo_save_complete", viewLifecycleOwner
        ) { _, bundle ->
            val savedCount = bundle.getInt("saved_count", 0)
            Toast.makeText(context, "$savedCount item(s) added to fridge", Toast.LENGTH_LONG).show()
            hideBlurredBackground()
            restartCamera()
        }
    }

    private fun loadLatestGalleryThumbnail() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resolver = requireContext().contentResolver
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                resolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val id = cursor.getLong(idColumn)
                        val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        withContext(Dispatchers.Main) {
                            _binding?.imgRecentPhoto?.setImageURI(uri)
                        }
                    }
                }
            } catch (_: Exception) {
            }
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
                val sampleSize = max(
                    options.outWidth / targetSize,
                    options.outHeight / targetSize
                ).coerceAtLeast(1)

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
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
                    stopCamera()
                    showBlurredBackground(safeBitmap)
                    if (isMultiMode) {
                        runMultiObjectDetection(safeBitmap)
                    } else {
                        runOllamaAnalysis(safeBitmap)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gallery image load failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private var isStartingCamera = false

    private fun startCamera() {
        if (isStartingCamera) return
        isStartingCamera = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
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

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                isCameraActive = true
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            } finally {
                isStartingCamera = false
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isCameraActive = false
    }

    private fun restartCamera() {
        startCamera()
        isCameraActive = true
    }

    private fun showBlurredBackground(bitmap: Bitmap) {
        val w = binding.previewView.width.coerceAtLeast(1)
        val h = binding.previewView.height.coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
        val blurred = Bitmap.createScaledBitmap(small, w, h, true)

        if (blurredBg == null) {
            blurredBg = ImageView(requireContext()).also {
                it.layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
                it.scaleType = ImageView.ScaleType.CENTER_CROP
                (binding.root as ViewGroup).addView(it, 0)
            }
        }
        blurredBg?.setImageBitmap(blurred)
        blurredBg?.visibility = View.VISIBLE
    }

    private fun hideBlurredBackground() {
        blurredBg?.visibility = View.GONE
    }

    private fun showCancelButton() {
        binding.captureButtonContainer.visibility = View.GONE
        binding.btnCancelBottom.visibility = View.VISIBLE
    }

    private fun hideCancelButton() {
        binding.btnCancelBottom.visibility = View.GONE
        binding.captureButtonContainer.visibility = View.VISIBLE
    }

    private fun captureAndAnalyze() {
        if (isProcessing) return

        val bitmap = latestBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
            return
        }

        val safeBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        if (safeBitmap == null) {
            Toast.makeText(context, "Image capture failed", Toast.LENGTH_SHORT).show()
            return
        }

        stopCamera()
        showBlurredBackground(bitmap)

        showFlashAnimation {
            if (isMultiMode) {
                runMultiObjectDetection(safeBitmap)
            } else {
                runOllamaAnalysis(safeBitmap)
            }
        }
    }

    private fun runMultiObjectDetection(bitmap: Bitmap) {
        isProcessing = true
        showProgressOverlay()
        binding.tvProgressTitle.text = "Detecting food items..."
        binding.tvProgressDetailOverlay.text = "Analyzing with YOLO + AI"
        binding.btnCancelProgress.visibility = View.VISIBLE

        detectionJob = scope.launch {
            detectionPipeline.detectAndClassify(bitmap).collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is PipelineState.Detecting -> {
                            binding.tvProgressTitle.text = "Detecting food items..."
                        }
                        is PipelineState.Detected -> {
                            if (BuildConfig.DEBUG) {
                                Log.d("OnnxYoloEngine-Debug", "Detected ${state.detections.size} items, starting classification")
                            }
                            binding.tvProgressTitle.text = "Items detected, classifying..."
                        }
                        is PipelineState.Classifying -> {
                            binding.tvProgressTitle.text = "Identifying item (${state.current}/${state.total})..."
                        }
                        is PipelineState.Complete -> {
                            hideProgressOverlay()
                            isProcessing = false
                            if (state.result.results.isNotEmpty()) {
                                val sessionId = state.result.sessionId
                                val entities = state.result.results.mapIndexed { index, result ->
                                    DetectionResultEntity(
                                        sessionId = sessionId,
                                        indexInSession = index,
                                        foodName = result.foodIdentification?.name ?: result.label,
                                        foodNameZh = result.foodIdentification?.nameZh ?: "",
                                        category = result.category.name,
                                        confidence = result.confidence,
                                        llmConfidence = result.foodIdentification?.confidence ?: 0f,
                                        status = when (result.status) {
                                            DetectionStatus.CLASSIFIED -> DetectionResultEntity.STATUS_CLASSIFIED
                                            DetectionStatus.FAILED -> DetectionResultEntity.STATUS_FAILED
                                            DetectionStatus.PENDING -> DetectionResultEntity.STATUS_PENDING
                                        },
                                        boundingBoxLeft = result.boundingBox.left,
                                        boundingBoxTop = result.boundingBox.top,
                                        boundingBoxRight = result.boundingBox.right,
                                        boundingBoxBottom = result.boundingBox.bottom,
                                        shelfLifeDays = result.foodIdentification?.shelfLifeDays,
                                        cropImagePath = result.cropImagePath
                                    )
                                }
                                detectionResultRepository.insertResults(entities)
                                val bundle = android.os.Bundle().apply {
                                    putString("sessionId", sessionId)
                                }
                                try {
                                    findNavController().navigate(
                                        R.id.action_scan_container_to_confirmation,
                                        bundle
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Navigation to confirmation failed", e)
                                    restartCamera()
                                }
                            } else {
                                Toast.makeText(context, "No food items detected", Toast.LENGTH_SHORT).show()
                                restartCamera()
                            }
                        }
                        is PipelineState.Error -> {
                            hideProgressOverlay()
                            isProcessing = false
                            Toast.makeText(context, "Detection failed: ${state.message}", Toast.LENGTH_LONG).show()
                            restartCamera()
                        }
                        is PipelineState.Cancelled -> {
                            hideProgressOverlay()
                            isProcessing = false
                            restartCamera()
                        }
                        is PipelineState.Idle -> {}
                    }
                }
            }
        }
    }

    private fun showFlashAnimation(onComplete: () -> Unit) {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 1f

        binding.flashOverlay.animate()
            .alpha(0.3f)
            .setDuration(150L)
            .withEndAction {
                showProgressOverlay()
                onComplete()
            }
            .start()
    }

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

    private fun cropToFocusRect(bitmap: Bitmap): Bitmap {
        val focusRect = binding.focusRectangle
        val preview = binding.previewView

        val previewW = preview.width
        val previewH = preview.height
        if (previewW <= 0 || previewH <= 0) return bitmap

        val rectW = focusRect.width
        val rectH = focusRect.height
        if (rectW <= 0 || rectH <= 0) return bitmap

        val location = IntArray(2)
        focusRect.getLocationOnScreen(location)
        val previewLocation = IntArray(2)
        preview.getLocationOnScreen(previewLocation)

        val rectLeft = location[0] - previewLocation[0]
        val rectTop = location[1] - previewLocation[1]

        val scaleX = bitmap.width.toFloat() / previewW.toFloat()
        val scaleY = bitmap.height.toFloat() / previewH.toFloat()

        val x = (rectLeft * scaleX).toInt().coerceIn(0, bitmap.width - 1)
        val y = (rectTop * scaleY).toInt().coerceIn(0, bitmap.height - 1)
        val w = (rectW * scaleX).toInt().coerceAtMost(bitmap.width - x)
        val h = (rectH * scaleY).toInt().coerceAtMost(bitmap.height - y)

        if (w <= 0 || h <= 0) return bitmap

        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    private fun runOllamaAnalysis(customBitmap: Bitmap? = null) {
        if (isProcessing) return

        var bitmap = customBitmap
        if (bitmap == null) {
            val rawBitmap = latestBitmap
            if (rawBitmap == null) {
                Toast.makeText(context, "No image captured", Toast.LENGTH_SHORT).show()
                return
            }
            bitmap = rawBitmap
        }

        bitmap = cropToFocusRect(bitmap)

        viewLifecycleOwner.lifecycleScope.launch {
            isProcessing = true
            showProgressOverlay()
            binding.tvProgressTitle.text = "Analyzing food..."
            binding.tvProgressDetailOverlay.text = "Sending to AI server..."
            updateStatus("Analyzing food...", Status.ANALYZING)
            startProgressTicker()
            showCancelButton()

            try {
                identifyFoodUseCase.invoke(bitmap).collect { result ->
                    isProcessing = false
                    stopProgressTicker()
                    hideProgressOverlay()
                    hideCancelButton()
                    binding.flashOverlay.animate().alpha(0f).setDuration(200L).withEndAction {
                        _binding?.flashOverlay?.visibility = View.GONE
                    }.start()

                    if (result.name == "Error" || result.name == "Unknown") {
                        displayAiResult(result.nameZh, result.expiryHint, result.rawResponse ?: "Error")
                        updateStatus("Analysis: ${result.name}", Status.ERROR)
                    } else {
                        ScanResultHolder.result = ScanResultHolder.ScanResult(
                            foodName = result.name,
                            foodNameZh = result.nameZh,
                            expiryHint = result.expiryHint,
                            confidence = result.confidence
                        )
                        try {
                            findNavController().popBackStack()
                        } catch (e: Exception) {
                            Log.e(TAG, "popBackStack failed after scan", e)
                            displayAiResult(result.name, result.expiryHint, result.rawResponse ?: "")
                            updateStatus("Analysis complete", Status.READY)
                        }
                    }
                }
            } catch (e: Exception) {
                isProcessing = false
                stopProgressTicker()
                hideProgressOverlay()
                hideCancelButton()
                binding.flashOverlay.visibility = View.GONE
                Log.e(TAG, "Analysis error", e)
                Toast.makeText(context, "AI 分析失敗: ${e.message}", Toast.LENGTH_LONG).show()
                updateStatus("Analysis failed", Status.ERROR)
            }
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        val start = System.currentTimeMillis()
        progressTickerJob = scope.launch {
            while (isActive && isProcessing) {
                val elapsedSec = (System.currentTimeMillis() - start) / 1000.0
                val stage = when {
                    elapsedSec < 3 -> "Sending image to server..."
                    elapsedSec < 10 -> "AI analyzing food..."
                    else -> "Almost done..."
                }
                _binding?.tvProgressDetailOverlay?.text = "$stage ${"%.1f".format(elapsedSec)}s"
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
        hideCancelButton()
        binding.flashOverlay.visibility = View.GONE
        hideBlurredBackground()
        restartCamera()
        Toast.makeText(requireContext(), "Analysis cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun displayAiResult(foodName: String?, expiryDate: String?, rawResponse: String) {
        binding.tvFoodName.text = "AI 識別: ${foodName ?: "未知"}"
        binding.tvExpiryDate.text = "有效日期: ${expiryDate ?: "未偵測到"}"
        binding.tvConfidence.visibility = View.GONE
        binding.tvRawResponse.text = rawResponse

        binding.resultCard.visibility = View.VISIBLE
        binding.rawResponseCard.visibility = View.VISIBLE
        updateStatus("AI 分析完成", Status.READY)
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
        progressTickerJob?.cancel()
        scope.cancel()
        try {
            val viewPager = try {
                parentFragment?.view?.findViewById<ViewPager2>(R.id.viewPager)
            } catch (_: Exception) { null }
            viewPagerCallback?.let { viewPager?.unregisterOnPageChangeCallback(it) }
        } catch (_: Exception) {}
        viewPagerCallback = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor.shutdownNow()
        blurredBg?.let { (_binding?.root as? ViewGroup)?.removeView(it) }
        blurredBg = null
        _binding = null
    }
}

object ScanResultHolder {
    data class ScanResult(
        val foodName: String,
        val foodNameZh: String,
        val expiryHint: String?,
        val confidence: Float
    )
    var result: ScanResult? = null
}

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