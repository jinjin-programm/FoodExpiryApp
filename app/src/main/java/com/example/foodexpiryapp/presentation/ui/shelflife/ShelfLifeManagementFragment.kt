package com.example.foodexpiryapp.presentation.ui.shelflife

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import com.example.foodexpiryapp.databinding.FragmentShelfLifeManagementBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShelfLifeManagementFragment : Fragment() {

    private var _binding: FragmentShelfLifeManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShelfLifeManagementViewModel by viewModels()

    private lateinit var adapter: ShelfLifeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentShelfLifeManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar()
        setupAdapter()
        setupChips()
        setupSearch()
        setupFab()
        observeEntries()
        observeStats()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupAdapter() {
        adapter = ShelfLifeAdapter(
            onConfirmClick = { entity -> viewModel.confirmEntry(entity.id) },
            onEditClick = { entity -> showEditDialog(entity) }
        )
        binding.recyclerShelfLife.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerShelfLife.adapter = adapter
    }

    private fun setupChips() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(ShelfLifeManagementViewModel.Filter.All)
        }
        binding.chipAuto.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(ShelfLifeManagementViewModel.Filter.Auto)
        }
        binding.chipManual.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setFilter(ShelfLifeManagementViewModel.Filter.Manual)
        }
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showAddDialog() }
    }

    private fun showAddDialog() {
        val dialog = ShelfLifeEditDialog()
        dialog.setCallbacks(
            onSave = { name, days, category, location ->
                viewModel.addNewEntry(name, days, category, location)
            }
        )
        dialog.show(childFragmentManager, "add_shelf_life")
    }

    private fun showEditDialog(entity: ShelfLifeEntity) {
        val dialog = ShelfLifeEditDialog()
        dialog.setExistingEntity(entity)
        dialog.setCallbacks(
            onSave = { _, _, _, _ ->
                val updated = entity.copy(
                    foodName = entity.foodName,
                    shelfLifeDays = entity.shelfLifeDays,
                    category = entity.category,
                    location = entity.location,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.addOrUpdateEntry(updated)
            },
            onDelete = { id -> viewModel.deleteEntry(id) }
        )
        dialog.show(childFragmentManager, "edit_shelf_life")
    }

    private fun observeEntries() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entries.collect { entries ->
                    adapter.submitList(entries)
                }
            }
        }
    }

    private fun observeStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statsText.collect { text ->
                    binding.tvStats.text = text
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
