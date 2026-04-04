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
import com.example.foodexpiryapp.data.repository.BarcodeRepository
import com.example.foodexpiryapp.databinding.DialogAddFoodBinding
import com.example.foodexpiryapp.databinding.FragmentInventoryBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.FoodCardAdapter
import com.example.foodexpiryapp.presentation.adapter.FoodListAdapter
import com.example.foodexpiryapp.presentation.viewmodel.InventoryEvent
import com.example.foodexpiryapp.presentation.viewmodel.InventoryViewModel
import com.example.foodexpiryapp.util.ShelfLifeEstimator
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class InventoryFragment : Fragment() {

    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels()

    @Inject
    lateinit var barcodeRepository: BarcodeRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private lateinit var expiringSoonAdapter: FoodCardAdapter
    private lateinit var freshStockAdapter: FoodListAdapter

    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    private var currentDialog: androidx.appcompat.app.AlertDialog? = null
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
        setupHeader()
        setupRecyclerViews()
        setupSearch()
        setupCategoryFilter()
        setupActionButtons()
        observeState()
        observeEvents()
        setupFragmentResultListeners()
    }

    private fun setupHeader() {
        binding.textCurrentDate.text = LocalDate.now().format(headerDateFormatter)
    }

    private fun setupRecyclerViews() {
        expiringSoonAdapter = FoodCardAdapter { item -> showAddEditDialog(item) }
        binding.recyclerExpiringSoon.apply {
            adapter = expiringSoonAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        freshStockAdapter = FoodListAdapter { item -> showAddEditDialog(item) }
        binding.foodItemsRecyclerView.apply {
            adapter = freshStockAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // Swipe to delete logic
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = freshStockAdapter.currentList[position]
                viewModel.onDeleteFoodItem(item)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.foodItemsRecyclerView)
    }

    private fun setupActionButtons() {
        binding.cardVisionScan.setOnClickListener {
            findNavController().currentDestination?.getAction(R.id.action_inventory_to_vision_scan)?.let {
                findNavController().navigate(R.id.action_inventory_to_vision_scan)
            }
        }
        binding.btnBarcodeScan.setOnClickListener {
            findNavController().currentDestination?.getAction(R.id.action_inventory_to_scan)?.let {
                val bundle = Bundle().apply { putString("scan_mode", "barcode") }
                findNavController().navigate(R.id.action_inventory_to_scan, bundle)
            }
        }
        binding.btnManualEntry.setOnClickListener {
            showAddEditDialog(null)
        }
        binding.btnPhotoScan.setOnClickListener {
            findNavController().currentDestination?.getAction(R.id.action_inventory_to_yolo_scan)?.let {
                findNavController().navigate(R.id.action_inventory_to_yolo_scan)
            }
        }
    }

    private fun setupCategoryFilter() {
        val chipGroup = binding.categoryChipGroup
        FoodCategory.values().forEach { category ->
            val chip = layoutInflater.inflate(R.layout.layout_filter_chip, chipGroup, false) as Chip
            chip.text = category.displayName
            chip.tag = category
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId == null || checkedId == R.id.chip_all) {
                viewModel.onCategorySelected(null)
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                viewModel.onCategorySelected(chip.tag as? FoodCategory)
            }
        }
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
                    val expiringSoon = state.foodItems.filter { it.daysUntilExpiry <= 3 }.sortedBy { it.daysUntilExpiry }
                    expiringSoonAdapter.submitList(expiringSoon)
                    
                    val otherItems = state.foodItems.sortedByDescending { it.dateAdded }
                    freshStockAdapter.submitList(otherItems)
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
                                .setAction("UNDO") { viewModel.onUndoDelete(event.item) }
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun setupFragmentResultListeners() {
        setFragmentResultListener("SCAN_RESULT") { _, bundle ->
            val barcode = bundle.getString("barcode")
            val dateString = bundle.getString("date")
            if (barcode != null) {
                handleBarcodeResult(barcode)
            } else if (dateString != null) {
                val expiryDate = parseDate(dateString) ?: LocalDate.now().plusDays(7)
                showAddEditDialog(draftFoodItem?.copy(expiryDate = expiryDate))
                draftFoodItem = null
            }
        }

        setFragmentResultListener("YOLO_SCAN_RESULT") { _, bundle ->
            val label = bundle.getString("yolo_label") ?: "Unknown Item"
            val category = FoodCategory.values().find { it.name == bundle.getString("yolo_category") } ?: FoodCategory.OTHER
            val shelfLife = ShelfLifeEstimator.estimateShelfLife(listOf(label))
            showAddEditDialog(FoodItem(
                name = label.replace("_", " ").capitalize(),
                category = category,
                expiryDate = ShelfLifeEstimator.calculateExpiryDate(shelfLife.days),
                quantity = 1,
                location = StorageLocation.FRIDGE,
                dateAdded = LocalDate.now()
            ))
        }

        setFragmentResultListener("llm_scan_result") { _, bundle ->
            val foodName = bundle.getString("food_name") ?: "Unknown"
            val expiryDateStr = bundle.getString("expiry_date")
            val expiryDate = parseDate(expiryDateStr) ?: ShelfLifeEstimator.calculateExpiryDate(ShelfLifeEstimator.estimateShelfLife(listOf(foodName.lowercase())).days)
            showAddEditDialog(FoodItem(
                name = foodName,
                expiryDate = expiryDate,
                category = FoodCategory.OTHER,
                location = StorageLocation.FRIDGE,
                quantity = 1,
                dateAdded = LocalDate.now(),
                notes = "AI Scan (Confidence: ${bundle.getString("confidence") ?: "medium"})"
            ))
        }
    }

    private fun handleBarcodeResult(barcode: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            barcodeRepository.scanBarcode(barcode).fold(
                onSuccess = { result ->
                    showAddEditDialog(FoodItem(
                        name = result.name,
                        barcode = barcode,
                        expiryDate = result.estimatedExpiryDate,
                        category = result.category,
                        location = StorageLocation.FRIDGE,
                        quantity = 1,
                        dateAdded = LocalDate.now(),
                        notes = result.brand?.let { "Brand: $it" } ?: ""
                    ))
                },
                onFailure = {
                    showAddEditDialog(FoodItem(
                        name = "Scanned Item",
                        barcode = barcode,
                        expiryDate = LocalDate.now().plusDays(7),
                        category = FoodCategory.OTHER,
                        location = StorageLocation.FRIDGE,
                        quantity = 1,
                        dateAdded = LocalDate.now()
                    ))
                }
            )
        }
    }

    private fun getCategoryDrawable(category: FoodCategory): Int {
        return when (category) {
            FoodCategory.DAIRY -> R.drawable.cat_dairy
            FoodCategory.MEAT -> R.drawable.cat_meat
            FoodCategory.VEGETABLES -> R.drawable.cat_vegetables
            FoodCategory.FRUITS -> R.drawable.cat_fruits
            FoodCategory.GRAINS -> R.drawable.cat_grains
            FoodCategory.BEVERAGES -> R.drawable.cat_beverages
            FoodCategory.SNACKS -> R.drawable.cat_snacks
            FoodCategory.CONDIMENTS -> R.drawable.cat_condiments
            FoodCategory.FROZEN -> R.drawable.cat_frozen
            FoodCategory.LEFTOVERS -> R.drawable.cat_leftovers
            FoodCategory.OTHER -> R.drawable.cat_other
        }
    }

    private fun updateCategoryImage(dialogBinding: DialogAddFoodBinding, category: FoodCategory) {
        Glide.with(requireContext())
            .load(getCategoryDrawable(category))
            .centerCrop()
            .into(dialogBinding.imgFoodCategory)
    }

    private fun showAddEditDialog(existingItem: FoodItem?) {
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        var selectedDate: LocalDate = existingItem?.expiryDate ?: LocalDate.now().plusDays(7)
        
        val initialCategory = existingItem?.category ?: FoodCategory.OTHER
        updateCategoryImage(dialogBinding, initialCategory)

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, FoodCategory.values().map { it.displayName })
        (dialogBinding.dropdownCategory as AutoCompleteTextView).apply {
            setAdapter(categoryAdapter)
            setText(initialCategory.displayName, false)
            // Ensure dropdown shows when clicked
            setOnClickListener { showDropDown() }
            
            // Update image dynamically when category is changed
            setOnItemClickListener { parent, _, position, _ ->
                val selectedName = parent.getItemAtPosition(position).toString()
                val newCategory = FoodCategory.values().find { it.displayName == selectedName } ?: FoodCategory.OTHER
                updateCategoryImage(dialogBinding, newCategory)
            }
        }

        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, StorageLocation.values().map { it.displayName })
        (dialogBinding.dropdownLocation as AutoCompleteTextView).apply {
            setAdapter(locationAdapter)
            setText(existingItem?.location?.displayName ?: StorageLocation.FRIDGE.displayName, false)
            // Ensure dropdown shows when clicked
            setOnClickListener { showDropDown() }
        }

        if (existingItem != null) {
            dialogBinding.editFoodName.setText(existingItem.name)
            dialogBinding.editQuantity.setText(existingItem.quantity.toString())
            dialogBinding.editNotes.setText(existingItem.notes)
            dialogBinding.editBarcode.setText(existingItem.barcode)
        }

        dialogBinding.editExpiryDate.setText(selectedDate.format(displayFormatter))
        dialogBinding.editExpiryDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                dialogBinding.editExpiryDate.setText(selectedDate.format(displayFormatter))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        dialogBinding.btnScanExpiry.setOnClickListener {
            draftFoodItem = existingItem?.copy(name = dialogBinding.editFoodName.text.toString())
            currentDialog?.dismiss()
            findNavController().currentDestination?.getAction(R.id.action_inventory_to_scan)?.let {
                findNavController().navigate(R.id.action_inventory_to_scan, Bundle().apply { putString("scan_mode", "date") })
            }
        }

        currentDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingItem?.id != 0L) "Edit Item" else "Add Item")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val categoryName = dialogBinding.dropdownCategory.text.toString()
                val category = FoodCategory.values().find { it.displayName == categoryName } ?: FoodCategory.OTHER
                
                val locationName = dialogBinding.dropdownLocation.text.toString()
                val location = StorageLocation.values().find { it.displayName == locationName } ?: StorageLocation.FRIDGE

                val item = (existingItem ?: FoodItem(name = "", category = category, expiryDate = selectedDate, quantity = 1, location = location, dateAdded = LocalDate.now())).copy(
                    name = dialogBinding.editFoodName.text.toString(),
                    category = category,
                    location = location,
                    quantity = dialogBinding.editQuantity.text.toString().toIntOrNull() ?: 1,
                    expiryDate = selectedDate,
                    notes = dialogBinding.editNotes.text.toString()
                )
                if (item.id != 0L) viewModel.onUpdateFoodItem(item) else viewModel.onAddFoodItem(item)
            }
            .setNegativeButton("Cancel", null)
            .create()
        currentDialog?.show()
    }

    private fun parseDate(dateString: String?): LocalDate? {
        if (dateString.isNullOrEmpty()) return null
        val cleanDate = dateString.replace(".", "/").replace("-", "/")
        val formats = listOf(
            DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"), DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"), DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )
        for (format in formats) {
            try { return LocalDate.parse(cleanDate, format) } catch (e: Exception) {}
        }
        return null
    }

    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentDialog = null
    }
}
