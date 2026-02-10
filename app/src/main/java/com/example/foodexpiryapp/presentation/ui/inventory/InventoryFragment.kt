package com.example.foodexpiryapp.presentation.ui.inventory

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.DialogAddFoodBinding
import com.example.foodexpiryapp.databinding.FragmentInventoryBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.presentation.adapter.FoodItemAdapter
import com.example.foodexpiryapp.presentation.viewmodel.InventoryEvent
import com.example.foodexpiryapp.presentation.viewmodel.InventoryViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@AndroidEntryPoint
class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    private lateinit var foodAdapter: FoodItemAdapter

    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    private var currentDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupFab()
        observeState()
        observeEvents()

        setFragmentResultListener("SCAN_RESULT") { _, bundle ->
            val barcode = bundle.getString("barcode")
            val dateString = bundle.getString("date")

            val expiryDate = parseDate(dateString) ?: LocalDate.now().plusDays(7)
            
            val newItem = FoodItem(
                name = if (barcode != null) "Scanned Item" else "",
                barcode = barcode,
                expiryDate = expiryDate,
                category = FoodCategory.OTHER,
                location = StorageLocation.FRIDGE,
                quantity = 1,
                dateAdded = LocalDate.now(),
                notes = ""
            )
            showAddEditDialog(newItem)
        }
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodItemAdapter { item ->
            showAddEditDialog(item)
        }

        binding.foodItemsRecyclerView.apply {
            adapter = foodAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Swipe to delete
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = foodAdapter.currentList[position]
                viewModel.onDeleteFoodItem(item)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.foodItemsRecyclerView)
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }
    }

    private fun setupFab() {
        binding.fabAddFood.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loading
                    binding.loadingProgress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    // Empty state
                    binding.emptyStateLayout.visibility =
                        if (!state.isLoading && state.foodItems.isEmpty()) View.VISIBLE else View.GONE

                    // List
                    binding.foodItemsRecyclerView.visibility =
                        if (!state.isLoading && state.foodItems.isNotEmpty()) View.VISIBLE else View.GONE

                    foodAdapter.submitList(state.foodItems)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is InventoryEvent.ShowMessage -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is InventoryEvent.ItemDeleted -> {
                            Snackbar.make(binding.root, "${event.item.name} deleted", Snackbar.LENGTH_LONG)
                                .setAction("UNDO") {
                                    viewModel.onUndoDelete(event.item)
                                }
                                .show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a dialog to add a new food item or edit an existing one.
     * @param existingItem null for add mode, non-null for edit mode.
     */
    private fun showAddEditDialog(existingItem: FoodItem?) {
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        var selectedDate: LocalDate = existingItem?.expiryDate ?: LocalDate.now().plusDays(7)

        // Set up category dropdown
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            FoodCategory.values().map { it.displayName }
        )
        (dialogBinding.dropdownCategory as AutoCompleteTextView).apply {
            setAdapter(categoryAdapter)
            setText(
                existingItem?.category?.displayName ?: FoodCategory.OTHER.displayName,
                false
            )
        }

        // Set up location dropdown
        val locationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            StorageLocation.values().map { it.displayName }
        )
        (dialogBinding.dropdownLocation as AutoCompleteTextView).apply {
            setAdapter(locationAdapter)
            setText(
                existingItem?.location?.displayName ?: StorageLocation.FRIDGE.displayName,
                false
            )
        }

        // Pre-fill fields if editing or scanned
        if (existingItem != null) {
            dialogBinding.editFoodName.setText(existingItem.name)
            dialogBinding.editQuantity.setText(existingItem.quantity.toString())
            dialogBinding.editNotes.setText(existingItem.notes)
            dialogBinding.editBarcode.setText(existingItem.barcode)
        }

        // Expiry date display
        dialogBinding.editExpiryDate.setText(selectedDate.format(displayFormatter))

        // Date picker on click
        dialogBinding.editExpiryDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    dialogBinding.editExpiryDate.setText(selectedDate.format(displayFormatter))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Also trigger date picker from the end icon
        dialogBinding.inputLayoutExpiry.setEndIconOnClickListener {
            dialogBinding.editExpiryDate.performClick()
        }

        // Scan button
        dialogBinding.btnScan.setOnClickListener {
            currentDialog?.dismiss()
            findNavController().navigate(R.id.action_inventory_to_scan)
        }

        val title = if (existingItem != null && existingItem.id != 0L) "Edit Food Item" else "Add Food Item"
        
        currentDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.editFoodName.text?.toString()?.trim() ?: ""
                if (name.isBlank()) {
                    Snackbar.make(binding.root, "Name is required", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val categoryName = dialogBinding.dropdownCategory.text.toString()
                val category = FoodCategory.values().find { it.displayName == categoryName }
                    ?: FoodCategory.OTHER

                val locationName = dialogBinding.dropdownLocation.text.toString()
                val location = StorageLocation.values().find { it.displayName == locationName }
                    ?: StorageLocation.FRIDGE

                val quantity = dialogBinding.editQuantity.text?.toString()?.toIntOrNull() ?: 1
                val notes = dialogBinding.editNotes.text?.toString()?.trim() ?: ""
                val barcode = dialogBinding.editBarcode.text?.toString()?.trim()

                val foodItem = FoodItem(
                    id = existingItem?.id ?: 0,
                    name = name,
                    category = category,
                    expiryDate = selectedDate,
                    quantity = quantity,
                    location = location,
                    notes = notes,
                    barcode = barcode,
                    dateAdded = existingItem?.dateAdded ?: LocalDate.now()
                )

                if (existingItem != null && existingItem.id != 0L) {
                    viewModel.onUpdateFoodItem(foodItem)
                } else {
                    viewModel.onAddFoodItem(foodItem)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        currentDialog?.show()
    }

    private fun parseDate(dateString: String?): LocalDate? {
        if (dateString.isNullOrEmpty()) return null
        
        // Try to handle dd/MM/yyyy, MM/dd/yyyy, yyyy-MM-dd
        // Also handle separators - / .
        val cleanDate = dateString.replace(".", "/").replace("-", "/")
        
        val formats = listOf(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )
        
        for (format in formats) {
            try {
                return LocalDate.parse(cleanDate, format)
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentDialog = null
    }
}
