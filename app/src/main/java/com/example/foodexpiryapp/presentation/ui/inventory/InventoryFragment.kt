package com.example.foodexpiryapp.presentation.ui.inventory

import android.app.DatePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.res.ResourcesCompat
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
import com.example.foodexpiryapp.data.repository.BarcodeRepository
import com.example.foodexpiryapp.databinding.DialogAddFoodBinding
import com.example.foodexpiryapp.databinding.FragmentInventoryBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.FoodItemAdapter
import com.example.foodexpiryapp.presentation.viewmodel.InventoryEvent
import com.example.foodexpiryapp.presentation.viewmodel.InventoryViewModel
import com.example.foodexpiryapp.util.ShelfLifeEstimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@AndroidEntryPoint
class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    @Inject
    lateinit var barcodeRepository: BarcodeRepository

    private lateinit var foodAdapter: FoodItemAdapter

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    private var currentDialog: androidx.appcompat.app.AlertDialog? = null

    // Holds the state of the item being edited when navigating away to scan expiry date
    private var draftFoodItem: FoodItem? = null

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
        setupSpeedDial()
        observeState()
        observeEvents()

        setFragmentResultListener("SCAN_RESULT") { _, bundle ->
            val barcode = bundle.getString("barcode")
            val dateString = bundle.getString("date")

            if (barcode != null) {
                draftFoodItem = null
                // Fetch product info from API
                viewLifecycleOwner.lifecycleScope.launch {
                    binding.loadingProgress.visibility = View.VISIBLE
                    barcodeRepository.scanBarcode(barcode).fold(
                        onSuccess = { result ->
                            binding.loadingProgress.visibility = View.GONE
                            
                            // Track analytics
                            analyticsRepository.trackEvent(
                                AnalyticsEvent(
                                    eventName = "scan_success",
                                    eventType = EventType.SCAN_SUCCESS,
                                    itemName = result.name,
                                    additionalData = mapOf("scan_type" to "barcode")
                                )
                            )

                            val newItem = FoodItem(
                                name = result.name,
                                barcode = barcode,
                                expiryDate = result.estimatedExpiryDate,
                                category = result.category,
                                location = StorageLocation.FRIDGE,
                                quantity = 1,
                                dateAdded = LocalDate.now(),
                                notes = result.brand?.let { "Brand: $it" } ?: ""
                            )
                            showAddEditDialog(newItem)
                        },
                        onFailure = { error ->
                            binding.loadingProgress.visibility = View.GONE
                            
                            // Track analytics
                            analyticsRepository.trackEvent(
                                AnalyticsEvent(
                                    eventName = "scan_failure",
                                    eventType = EventType.SCAN_FAILURE,
                                    additionalData = mapOf("scan_type" to "barcode", "error" to (error.message ?: "unknown"))
                                )
                            )

                            // Fallback to manual entry
                            val newItem = FoodItem(
                                name = "Scanned Item",
                                barcode = barcode,
                                expiryDate = LocalDate.now().plusDays(7),
                                category = FoodCategory.OTHER,
                                location = StorageLocation.FRIDGE,
                                quantity = 1,
                                dateAdded = LocalDate.now(),
                                notes = ""
                            )
                            showAddEditDialog(newItem)
                            Snackbar.make(binding.root, "Could not fetch product info: ${error.message}", Snackbar.LENGTH_LONG).show()
                        }
                    )
                }
            } else if (dateString != null) {
                val expiryDate = parseDate(dateString) ?: LocalDate.now().plusDays(7)
                
                val itemToShow = draftFoodItem?.copy(expiryDate = expiryDate) ?: FoodItem(
                    name = "",
                    category = FoodCategory.OTHER,
                    expiryDate = expiryDate,
                    quantity = 1,
                    location = StorageLocation.FRIDGE,
                    notes = "",
                    barcode = null,
                    dateAdded = LocalDate.now()
                )
                
                draftFoodItem = null
                showAddEditDialog(itemToShow)
            }
        }

        // Listen for YOLO scan results
        setFragmentResultListener("YOLO_SCAN_RESULT") { _, bundle ->
            val label = bundle.getString("yolo_label") ?: "Unknown Item"
            
            // Track analytics
            analyticsRepository.trackEvent(
                AnalyticsEvent(
                    eventName = "scan_success",
                    eventType = EventType.SCAN_SUCCESS,
                    itemName = label,
                    additionalData = mapOf("scan_type" to "yolo")
                )
            )

            val categoryName = bundle.getString("yolo_category")
            val category = categoryName?.let { name ->
                FoodCategory.values().find { it.name == name }
            } ?: FoodCategory.OTHER

            // Estimate smart expiry date based on the specific item label
            val shelfLife = ShelfLifeEstimator.estimateShelfLife(listOf(label))
            val calculatedExpiryDate = ShelfLifeEstimator.calculateExpiryDate(shelfLife.days)

            draftFoodItem = null
            
            // Format label for display (e.g. "tomato_sauce" -> "Tomato Sauce")
            val formattedName = label.replace("_", " ").split(" ").joinToString(" ") { 
                it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } 
            }
            
            val newItem = FoodItem(
                name = formattedName,
                category = category,
                expiryDate = calculatedExpiryDate,
                quantity = 1,
                location = StorageLocation.FRIDGE,
                notes = "",
                barcode = null,
                dateAdded = LocalDate.now()
            )
            
            showAddEditDialog(newItem)
            Snackbar.make(binding.root, "Detected: $formattedName (${shelfLife.days} days)", Snackbar.LENGTH_SHORT).show()
        }

        // Listen for LLM AI scan results (Qwen3.5)
        setFragmentResultListener("llm_scan_result") { _, bundle ->
            val foodName = bundle.getString("food_name") ?: "Unknown"
            
            // Track analytics
            analyticsRepository.trackEvent(
                AnalyticsEvent(
                    eventName = "scan_success",
                    eventType = EventType.SCAN_SUCCESS,
                    itemName = foodName,
                    additionalData = mapOf("scan_type" to "llm")
                )
            )

            val expiryDateStr = bundle.getString("expiry_date")
            val confidence = bundle.getString("confidence") ?: "medium"

            val expiryDate = if (!expiryDateStr.isNullOrBlank() && expiryDateStr != "not visible") {
                parseDate(expiryDateStr) ?: LocalDate.now().plusDays(7)
            } else {
                // Estimate based on food type
                val shelfLife = ShelfLifeEstimator.estimateShelfLife(listOf(foodName.lowercase()))
                ShelfLifeEstimator.calculateExpiryDate(shelfLife.days)
            }

            draftFoodItem = null

            val newItem = FoodItem(
                name = foodName,
                barcode = null,
                expiryDate = expiryDate,
                category = FoodCategory.OTHER,
                location = StorageLocation.FRIDGE,
                quantity = 1,
                dateAdded = LocalDate.now(),
                notes = "AI Scan (Confidence: $confidence)"
            )

            showAddEditDialog(newItem)
            Snackbar.make(binding.root, "AI Detected: $foodName", Snackbar.LENGTH_SHORT).show()
        }
}

private fun setupSpeedDial() {
        val speedDial = binding.speedDial
        
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.btn_vision_scan, android.R.drawable.ic_menu_gallery)
                .setLabel("Vision Scan (AI)")
                .setFabBackgroundColor(ResourcesCompat.getColor(resources, R.color.purple_500, null))
                .setFabImageTintColor(Color.WHITE)
                .create()
        )
        
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.btn_photo, android.R.drawable.ic_menu_camera)
                .setLabel("Photo Scan")
                .setFabBackgroundColor(ResourcesCompat.getColor(resources, R.color.purple_500, null))
                .setFabImageTintColor(Color.WHITE)
                .create()
        )
        
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.btn_barcode, R.drawable.ic_barcode)
                .setLabel("Barcode")
                .setFabBackgroundColor(ResourcesCompat.getColor(resources, R.color.purple_500, null))
                .setFabImageTintColor(Color.WHITE)
                .create()
        )
        
        speedDial.addActionItem(
            SpeedDialActionItem.Builder(R.id.btn_write, android.R.drawable.ic_menu_edit)
                .setLabel("Manual Entry")
                .setFabBackgroundColor(ResourcesCompat.getColor(resources, R.color.purple_500, null))
                .setFabImageTintColor(Color.WHITE)
                .create()
        )
        
        speedDial.setOnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.btn_vision_scan -> {
                    findNavController().navigate(R.id.action_inventory_to_vision_scan)
                }
                R.id.btn_photo -> {
                    draftFoodItem = null
                    findNavController().navigate(R.id.action_inventory_to_yolo_scan)
                }
                R.id.btn_barcode -> {
                    draftFoodItem = null
                    val bundle = android.os.Bundle().apply {
                        putString("scan_mode", "barcode")
                    }
                    findNavController().navigate(R.id.action_inventory_to_scan, bundle)
                }
                R.id.btn_write -> {
                    draftFoodItem = null
                    showAddEditDialog(null)
                }
            }
            speedDial.close()
            true
        }
    }

    private fun setupRecyclerView() {
        foodAdapter = FoodItemAdapter(
            onItemClick = { item ->
                showAddEditDialog(item)
            },
            onEatenToggle = { item, isEaten ->
                if (isEaten) {
                    viewModel.onMarkAsEaten(item)
                }
            }
        )

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

    override fun onResume() {
        super.onResume()
        analyticsRepository.trackEvent(
            AnalyticsEvent(
                eventName = "screen_view",
                eventType = EventType.SCREEN_VIEW,
                additionalData = mapOf("screen_name" to "inventory")
            )
        )
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
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

        // Scan Expiry Button
        dialogBinding.btnScanExpiry.setOnClickListener {
            val name = dialogBinding.editFoodName.text?.toString()?.trim() ?: ""
            val categoryName = dialogBinding.dropdownCategory.text.toString()
            val category = FoodCategory.values().find { it.displayName == categoryName }
                ?: FoodCategory.OTHER

            val locationName = dialogBinding.dropdownLocation.text.toString()
            val location = StorageLocation.values().find { it.displayName == locationName }
                ?: StorageLocation.FRIDGE

            val quantity = dialogBinding.editQuantity.text?.toString()?.toIntOrNull() ?: 1
            val notes = dialogBinding.editNotes.text?.toString()?.trim() ?: ""
            val barcode = dialogBinding.editBarcode.text?.toString()?.trim()

            draftFoodItem = FoodItem(
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

            currentDialog?.dismiss()
            val bundle = android.os.Bundle().apply {
                putString("scan_mode", "date")
            }
            findNavController().navigate(R.id.action_inventory_to_scan, bundle)
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

// Extension function to capitalize first letter
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
