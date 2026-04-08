package com.example.foodexpiryapp.presentation.ui.detection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.databinding.FragmentConfirmationBinding
import com.example.foodexpiryapp.presentation.adapter.DetectionResultAdapter
import com.example.foodexpiryapp.presentation.viewmodel.ConfirmationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Full-screen confirmation page for batch detection results.
 *
 * Per D-09: Top bar with back + title + count, scrollable item list, bottom fixed bar.
 * Per D-10: Single-item optimization — button says "Add to Fridge".
 * Per D-11: Failed items with orange background and "Fix" button.
 * Per D-12: Quick Mode auto-confirm countdown for single items.
 * Per D-14: Save flow via ViewModel → SaveDetectedFoodsUseCase.
 * Per D-15: After save, FragmentResult carries result back to YoloScanFragment for Snackbar.
 * Per D-16: Navigation Component + Safe Args with sessionId.
 * Per D-20: Reads results from Room DB — survives process death.
 */
@AndroidEntryPoint
class ConfirmationFragment : Fragment() {

    private var _binding: FragmentConfirmationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConfirmationViewModel by viewModels()

    private lateinit var adapter: DetectionResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Read sessionId from nav args (D-16)
        val sessionId = arguments?.getString("sessionId") ?: run {
            findNavController().popBackStack()
            return
        }

        setupAdapter()
        setupClickListeners()
        observeResults()
        observeSaveResult()

        // Per D-12: Any user interaction cancels Quick Mode countdown
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                viewModel.cancelQuickModeCountdown()
            }
            false
        }
    }

    private fun setupAdapter() {
        adapter = DetectionResultAdapter(
            onEditClick = { entity ->
                viewModel.cancelQuickModeCountdown()
                viewModel.updateItemName(entity.id, entity.foodName, entity.foodNameZh)
            },
            onDeleteClick = { entity ->
                viewModel.cancelQuickModeCountdown()
                viewModel.removeItem(entity.id)
            }
        )
        binding.recyclerResults.adapter = adapter
    }

    private fun setupClickListeners() {
        // Back button — clear session and navigate back
        binding.btnBack.setOnClickListener {
            viewModel.clearSession()
            findNavController().popBackStack()
        }

        // Cancel button — clear session and navigate back
        binding.btnCancel.setOnClickListener {
            viewModel.cancelQuickModeCountdown()
            viewModel.clearSession()
            findNavController().popBackStack()
        }

        // Per D-14: Add All to Fridge button — triggers save via SaveDetectedFoodsUseCase
        binding.btnAddAll.setOnClickListener {
            viewModel.cancelQuickModeCountdown()
            val activeResults = viewModel.getActiveResults()
            if (activeResults.isEmpty()) {
                Toast.makeText(context, "No items to add", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveAll()
        }
    }

    /**
     * Observe results from Room DB reactively.
     * Per D-20: Survives process death.
     * Per D-12: Starts Quick Mode countdown if enabled + exactly 1 item.
     */
    private fun observeResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { results ->
                    val active = results.filter { it.status != "REMOVED" }
                    adapter.submitList(active)
                    binding.tvTitle.text = "Confirm Items (${active.size})"

                    // D-10: Single-item optimization
                    if (active.size == 1) {
                        binding.btnAddAll.text = "Add to Fridge"
                    } else {
                        binding.btnAddAll.text = "Add All to Fridge"
                    }

                    // D-12: Start Quick Mode countdown if applicable
                    viewModel.startQuickModeCountdownIfNeeded()
                }
            }
        }

        // Observe Quick Mode countdown for button text update
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quickModeCountdown.collect { countdown ->
                    if (countdown > 0) {
                        binding.btnAddAll.text = "Auto-add in ${countdown}s..."
                    }
                }
            }
        }
    }

    /**
     * Per D-14: Observe save result and navigate back with result data.
     * Per D-15: Pass saved_count and session_id via FragmentResult for Snackbar in YoloScanFragment.
     */
    private fun observeSaveResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveResult.collect { result ->
                    if (result != null) {
                        // Pass back to YoloScanFragment via FragmentResult (D-15)
                        requireActivity().supportFragmentManager.setFragmentResult(
                            "yolo_save_complete",
                            bundleOf(
                                "saved_count" to result.savedCount,
                                "session_id" to result.sessionId
                            )
                        )
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Per D-12: Countdown cancels on onStop()
        viewModel.cancelQuickModeCountdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
