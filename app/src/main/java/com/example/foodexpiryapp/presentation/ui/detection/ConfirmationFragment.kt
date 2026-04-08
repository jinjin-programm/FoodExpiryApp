package com.example.foodexpiryapp.presentation.ui.detection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        setupClickListeners(sessionId)
        observeResults()
    }

    private fun setupAdapter() {
        adapter = DetectionResultAdapter(
            onEditClick = { entity ->
                viewModel.updateItemName(entity.id, entity.foodName, entity.foodNameZh)
            },
            onDeleteClick = { entity ->
                viewModel.removeItem(entity.id)
            }
        )
        binding.recyclerResults.adapter = adapter
    }

    private fun setupClickListeners(sessionId: String) {
        // Back button — clear session and navigate back
        binding.btnBack.setOnClickListener {
            viewModel.clearSession()
            findNavController().popBackStack()
        }

        // Cancel button — clear session and navigate back
        binding.btnCancel.setOnClickListener {
            viewModel.clearSession()
            findNavController().popBackStack()
        }

        // Add All to Fridge button — pass results back via FragmentResult
        // Note: actual save to FoodItem table is deferred to Plan 04
        binding.btnAddAll.setOnClickListener {
            val activeResults = viewModel.getActiveResults()
            if (activeResults.isEmpty()) {
                Toast.makeText(context, "No items to add", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("sessionId", sessionId)
                putInt("item_count", activeResults.size)
            }
            requireActivity().supportFragmentManager.setFragmentResult("confirmation_complete", bundle)
            findNavController().popBackStack()
        }
    }

    /**
     * Observe results from Room DB reactively.
     * Per D-20: Survives process death.
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
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
