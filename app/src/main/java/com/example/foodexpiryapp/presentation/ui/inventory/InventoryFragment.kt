package com.example.foodexpiryapp.presentation.ui.inventory

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.animation.AnimationUtils
import android.widget.PopupMenu
import android.widget.Toast
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
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
import com.example.foodexpiryapp.databinding.FragmentInventoryBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.domain.repository.UIStyleRepository
import com.example.foodexpiryapp.presentation.adapter.ExpiringCuteAdapter
import com.example.foodexpiryapp.presentation.adapter.FoodCardAdapter
import com.example.foodexpiryapp.presentation.adapter.FoodItemCuteAdapter
import com.example.foodexpiryapp.presentation.adapter.FoodListAdapter
import com.example.foodexpiryapp.presentation.viewmodel.InventoryEvent
import com.example.foodexpiryapp.presentation.viewmodel.InventoryViewModel
import com.example.foodexpiryapp.util.ShelfLifeEstimator
import com.example.foodexpiryapp.util.FoodImageResolver
import com.example.foodexpiryapp.util.FoodImageStorage
import com.example.foodexpiryapp.presentation.ui.vision.ScanResultHolder
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

    @Inject
    lateinit var foodImageStorage: FoodImageStorage

    private var pendingScanImageBytes: ByteArray? = null

    private lateinit var expiringOriginalAdapter: FoodCardAdapter
    private lateinit var expiringCuteAdapter: ExpiringCuteAdapter
    private lateinit var foodListOriginalAdapter: FoodListAdapter
    private lateinit var foodListCuteAdapter: FoodItemCuteAdapter

    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    private var currentDialog: androidx.appcompat.app.AlertDialog? = null
    private var draftFoodItem: FoodItem? = null
    private var currentStyle: String = ""

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
        setupLocationFilter()
        setupActionButtons()
        observeState()
        observeEvents()
        setupFragmentResultListeners()
    }

    private fun setupHeader() {
        binding.textCurrentDate.text = LocalDate.now().format(headerDateFormatter)
    }

    private fun setupRecyclerViews() {
        val onFoodClick: (FoodItem) -> Unit = { item -> showAddEditDialog(item) }

        expiringOriginalAdapter = FoodCardAdapter(onFoodClick)
        expiringCuteAdapter = ExpiringCuteAdapter(onFoodClick)

        foodListOriginalAdapter = FoodListAdapter(onFoodClick)
        foodListCuteAdapter = FoodItemCuteAdapter(onFoodClick)

        binding.recyclerExpiringSoon.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        binding.foodItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.foodItemsRecyclerView.adapter
                if (adapter is FoodListAdapter) {
                    val position = viewHolder.bindingAdapterPosition
                    val item = adapter.currentList[position]
                    viewModel.onDeleteFoodItem(item)
                } else if (adapter is FoodItemCuteAdapter) {
                    val position = viewHolder.bindingAdapterPosition
                    val item = adapter.currentList[position]
                    viewModel.onDeleteFoodItem(item)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.foodItemsRecyclerView)
    }

    private fun setupActionButtons() {
        binding.btnTryAIScan.setOnClickListener {
            navWithDebug(R.id.action_inventory_to_yolo_scan, "btnTryAIScan")
        }

        binding.cardPhoto.setOnClickListener {
            navWithDebug(R.id.action_inventory_to_yolo_scan, "cardPhoto")
        }

        binding.cardBarcode.setOnClickListener {
            try {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.navigation_inventory) {
                    val bundle = Bundle().apply { putString("scan_mode", "barcode") }
                    navController.navigate(R.id.action_inventory_to_scan, bundle)
                }
            } catch (e: Exception) {
                Log.e("NAV_DEBUG", "Exception barcode scan", e)
            }
        }

        binding.cardManual.setOnClickListener {
            showAddEditDialog(null)
        }

        binding.cardAiChat.setOnClickListener {
            navWithDebug(R.id.action_inventory_to_chat, "cardAiChat")
        }

        binding.btnBarcodeScan.setOnClickListener {
            try {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.navigation_inventory) {
                    val bundle = Bundle().apply { putString("scan_mode", "barcode") }
                    navController.navigate(R.id.action_inventory_to_scan, bundle)
                }
            } catch (e: Exception) {
                Log.e("NAV_DEBUG", "Exception barcode scan", e)
            }
        }
        binding.btnManualEntry.setOnClickListener {
            showAddEditDialog(null)
        }
        binding.btnPhotoScan.setOnClickListener {
            navWithDebug(R.id.action_inventory_to_yolo_scan, "btnPhotoScan")
        }
        binding.btnEmptyStateScan.setOnClickListener {
            try {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.navigation_inventory) {
                    val bundle = Bundle().apply { putString("scan_mode", "barcode") }
                    navController.navigate(R.id.action_inventory_to_scan, bundle)
                }
            } catch (e: Exception) {
                Log.e("NAV_DEBUG", "Exception empty state scan", e)
            }
        }

        binding.fabQuickActions.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.inventory_quick_actions, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_photo_scan -> {
                        navWithDebug(R.id.action_inventory_to_yolo_scan, "fab-photo_scan")
                        true
                    }
                    R.id.action_manual_entry -> {
                        showAddEditDialog(null)
                        true
                    }
                    R.id.action_view_expiring -> {
                        Toast.makeText(requireContext(), "View Expiring Not Implemented", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_view_analysis -> {
                        Toast.makeText(requireContext(), "View Analysis Not Implemented", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_load_test_data -> {
                        viewModel.onInsertTestData()
                        true
                    }
                    R.id.action_clear_all_data -> {
                        viewModel.onDeleteAllFoodItems()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun applyStyle(style: String) {
        val isCute = style == UIStyleRepository.STYLE_CUTE

        if (isCute) {
            binding.heroBanner.apply {
                cardElevation = 8f
                setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                background = null
                setBackgroundResource(R.drawable.bg_hero_gradient)
            }
            binding.newBadge.visibility = View.VISIBLE
            binding.imgRobot.visibility = View.VISIBLE
            binding.imgRobot.startAnimation(
                AnimationUtils.loadAnimation(requireContext(), R.anim.float_animation)
            )
            binding.textHeroTitle.setTextColor(android.graphics.Color.WHITE)
            binding.textHeroSubtitle.setTextColor(android.graphics.Color.parseColor("#CCFFFFFF"))
            binding.btnTryAIScan.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.WHITE
            )
            binding.btnTryAIScan.setTextColor(android.graphics.Color.parseColor("#4D644F"))
            binding.btnTryAIScan.iconTint = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4D644F")
            )

            binding.iconPhotoCircle.setBackgroundResource(R.drawable.bg_quick_action_circle_green)
            binding.iconBarcodeCircle.setBackgroundResource(R.drawable.bg_quick_action_circle_blue)
            binding.iconManualCircle.setBackgroundResource(R.drawable.bg_quick_action_circle_orange)
            binding.iconAiChatCircle.setBackgroundResource(R.drawable.bg_quick_action_circle_purple)

            startPulseAnimation()

            binding.imgEmptyIllustration.setImageResource(R.drawable.ic_cat_empty)
            binding.imgEmptyIllustration.imageTintList = null
            binding.textEmptyTitle.text = "Your kitchen is empty!"
            binding.textEmptySubtitle.text = "Let's fill it up \uD83D\uDC31"

            binding.recyclerExpiringSoon.adapter = expiringCuteAdapter
            binding.foodItemsRecyclerView.adapter = foodListCuteAdapter

            binding.searchBarLayout.setBoxBackgroundColorResource(R.color.search_bar_bg_cute)
            binding.searchBarLayout.setBackgroundColor(Color.TRANSPARENT)
            binding.searchEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.search_bar_text_cute))
            binding.searchEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.search_bar_hint_cute))
            binding.searchBarLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.search_bar_icon_cute)))
            binding.searchBarLayout.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.search_bar_icon_cute)))
        } else {
            binding.heroBanner.apply {
                cardElevation = 2f
                background = null
                setCardBackgroundColor(android.graphics.Color.WHITE)
            }
            binding.newBadge.visibility = View.GONE
            binding.imgRobot.visibility = View.GONE
            binding.imgRobot.clearAnimation()
            binding.textHeroTitle.setTextColor(android.graphics.Color.parseColor("#171725"))
            binding.textHeroSubtitle.setTextColor(android.graphics.Color.parseColor("#6B7280"))
            binding.btnTryAIScan.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#4D644F")
            )
            binding.btnTryAIScan.setTextColor(android.graphics.Color.WHITE)
            binding.btnTryAIScan.iconTint = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.WHITE
            )

            binding.iconPhotoCircle.background = null
            binding.iconBarcodeCircle.background = null
            binding.iconManualCircle.background = null
            binding.iconAiChatCircle.background = null

            stopPulseAnimation()

            binding.imgEmptyIllustration.setImageResource(android.R.drawable.ic_menu_camera)
            binding.imgEmptyIllustration.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#D1D5DB")
            )
            binding.textEmptyTitle.text = "Get Started"
            binding.textEmptySubtitle.text = "Add your first item using a quick photo scan to get started tracking."

            binding.recyclerExpiringSoon.adapter = expiringOriginalAdapter
            binding.foodItemsRecyclerView.adapter = foodListOriginalAdapter

            binding.searchBarLayout.setBoxBackgroundColorResource(R.color.search_bar_bg_original)
            binding.searchBarLayout.setBackgroundColor(Color.TRANSPARENT)
            binding.searchEditText.setTextColor(ContextCompat.getColor(requireContext(), R.color.search_bar_text_original))
            binding.searchEditText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.search_bar_hint_original))
            binding.searchBarLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.search_bar_icon_original)))
            binding.searchBarLayout.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.search_bar_icon_original)))
        }
    }

    private fun startPulseAnimation() {
        val scaleAnim = android.view.animation.ScaleAnimation(
            1f, 1.05f, 1f, 1.05f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnim.duration = 750
        scaleAnim.repeatMode = android.view.animation.Animation.REVERSE
        scaleAnim.repeatCount = android.view.animation.Animation.INFINITE
        scaleAnim.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        binding.cardPhoto.startAnimation(scaleAnim)
    }

    private fun stopPulseAnimation() {
        binding.cardPhoto.clearAnimation()
    }

    private fun setupLocationFilter() {
        val chipGroup = binding.categoryChipGroup

        val allChip = layoutInflater.inflate(R.layout.layout_filter_chip, chipGroup, false) as Chip
        allChip.text = "All"
        allChip.id = R.id.chip_all
        allChip.isChecked = true
        chipGroup.addView(allChip)

        StorageLocation.values().forEach { location ->
            val chip = layoutInflater.inflate(R.layout.layout_filter_chip, chipGroup, false) as Chip
            chip.text = location.displayName
            chip.tag = location
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId == null || checkedId == R.id.chip_all) {
                viewModel.onLocationSelected(null)
            } else {
                val chip = group.findViewById<Chip>(checkedId)
                viewModel.onLocationSelected(chip.tag as? StorageLocation)
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
                    if (currentStyle != state.uiStyle) {
                        currentStyle = state.uiStyle
                        applyStyle(state.uiStyle)
                    }

                    val isEmpty = state.foodItems.isEmpty()

                    binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.layoutExpiringSoonHeader.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.recyclerExpiringSoon.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.layoutFreshStockHeader.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.foodItemsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.searchBarLayout.visibility = if (isEmpty) View.GONE else View.VISIBLE

                    val expiringSoon = state.foodItems.filter { it.daysUntilExpiry <= 7 }.sortedBy { it.daysUntilExpiry }
                    val otherItems = state.foodItems.sortedByDescending { it.dateAdded }

                    val isCute = state.uiStyle == UIStyleRepository.STYLE_CUTE
                    if (isCute) {
                        if (expiringSoon.isEmpty()) {
                            binding.recyclerExpiringSoon.stopAutoScroll()
                            binding.recyclerExpiringSoon.cancelResumeTimer()
                            expiringCuteAdapter.infiniteMode = false
                            binding.recyclerExpiringSoon.scrollToPosition(0)
                        }
                        expiringCuteAdapter.infiniteMode = expiringSoon.size >= 2
                        expiringCuteAdapter.submitList(expiringSoon) {
                            if (expiringCuteAdapter.infiniteMode && expiringSoon.isNotEmpty()) {
                                binding.recyclerExpiringSoon.scrollToMiddlePosition()
                                binding.recyclerExpiringSoon.startAutoScroll()
                            }
                        }
                        foodListCuteAdapter.submitList(otherItems)
                    } else {
                        binding.recyclerExpiringSoon.stopAutoScroll()
                        binding.recyclerExpiringSoon.cancelResumeTimer()
                        expiringOriginalAdapter.submitList(expiringSoon)
                        foodListOriginalAdapter.submitList(otherItems)
                    }
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
        requireActivity().supportFragmentManager.setFragmentResultListener("SCAN_RESULT", viewLifecycleOwner) { _, bundle ->
            val barcode = bundle.getString("barcode")
            val dateString = bundle.getString("date")
            if (barcode != null) {
                handleBarcodeResult(barcode)
            } else if (dateString != null) {
                val bottomSheet = AddFoodBottomSheet.newInstance(expiryDate = dateString)
                bottomSheet.show(requireActivity().supportFragmentManager, AddFoodBottomSheet.TAG)
            }
        }

        requireActivity().supportFragmentManager.setFragmentResultListener("YOLO_SCAN_RESULT", viewLifecycleOwner) { _, bundle ->
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

        requireActivity().supportFragmentManager.setFragmentResultListener("llm_scan_result", viewLifecycleOwner) { _, bundle ->
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
                notes = "AI Scan (Confidence: ${String.format("%.0f%%", bundle.getFloat("confidence", 0f) * 100)})"
            ))
        }
    }

    private fun handleBarcodeResult(barcode: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            barcodeRepository.scanBarcode(barcode).fold(
                onSuccess = { result ->
                    val bottomSheet = AddFoodBottomSheet.newInstance(
                        barcode = barcode,
                        foodName = result.name,
                        category = result.category.name,
                        expiryDate = result.estimatedExpiryDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        notes = result.brand?.let { "Brand: $it" } ?: ""
                    )
                    bottomSheet.show(requireActivity().supportFragmentManager, AddFoodBottomSheet.TAG)
                },
                onFailure = {
                    val bottomSheet = AddFoodBottomSheet.newInstance(
                        barcode = barcode
                    )
                    bottomSheet.show(requireActivity().supportFragmentManager, AddFoodBottomSheet.TAG)
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

    private fun updateCategoryImage(dialogBinding: com.example.foodexpiryapp.databinding.DialogAddFoodBinding, category: FoodCategory, foodName: String = "", imagePath: String? = null) {
        val glideRequest = if (!imagePath.isNullOrBlank()) {
            val file = java.io.File(imagePath)
            if (file.exists()) {
                Glide.with(requireContext()).load(file)
            } else {
                Glide.with(requireContext()).load(FoodImageResolver.getFoodImage(foodName.ifBlank { category.displayName }, category))
            }
        } else {
            Glide.with(requireContext()).load(FoodImageResolver.getFoodImage(foodName.ifBlank { category.displayName }, category))
        }
        glideRequest.centerCrop().into(dialogBinding.imgFoodCategory)
    }

    private fun showAddEditDialog(existingItem: FoodItem?) {
        val dialogBinding = com.example.foodexpiryapp.databinding.DialogAddFoodBinding.inflate(layoutInflater)
        var selectedDate: LocalDate = existingItem?.expiryDate ?: LocalDate.now().plusDays(7)

        val initialCategory = existingItem?.category ?: FoodCategory.OTHER
        updateCategoryImage(dialogBinding, initialCategory, existingItem?.name ?: "", existingItem?.imagePath)

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, FoodCategory.values().map { it.displayName })
        (dialogBinding.dropdownCategory as AutoCompleteTextView).apply {
            setAdapter(categoryAdapter)
            setText(initialCategory.displayName, false)
            setOnClickListener { showDropDown() }
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
            setOnClickListener { showDropDown() }
        }

        if (existingItem != null) {
            dialogBinding.editFoodName.setText(existingItem.name)
            dialogBinding.editQuantity.setText(existingItem.quantity.toString())
            dialogBinding.editNotes.setText(existingItem.notes)
            dialogBinding.editBarcode.setText(existingItem.barcode)
        }

        dialogBinding.editFoodName.doAfterTextChanged {
            val name = it?.toString()?.trim() ?: ""
            val cat = FoodCategory.values().find { c -> c.displayName == dialogBinding.dropdownCategory.text.toString() } ?: FoodCategory.OTHER
            updateCategoryImage(dialogBinding, cat, name, existingItem?.imagePath)
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
                if (item.id != 0L) {
                    viewModel.onUpdateFoodItem(item)
                } else {
                    val imageBytes = pendingScanImageBytes
                    pendingScanImageBytes = null
                    if (imageBytes != null) {
                        viewModel.onAddFoodItemWithImage(item, imageBytes)
                    } else {
                        viewModel.onAddFoodItem(item)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        currentDialog?.show()
    }

    private fun logNavState(tag: String) {
        try {
            val navController = findNavController()
            val currentDest = navController.currentDestination
            Log.d("NAV_DEBUG", "===== $tag =====")
            Log.d("NAV_DEBUG", "  Current destination: id=${currentDest?.id}, label=${currentDest?.label}, nav_inventory=${R.id.navigation_inventory}")
            Log.d("NAV_DEBUG", "  Guard match: ${currentDest?.id == R.id.navigation_inventory}")
        } catch (e: Exception) {
            Log.e("NAV_DEBUG", "  logNavState FAILED", e)
        }
    }

    private fun navWithDebug(actionId: Int, buttonName: String) {
        Log.d("NAV_DEBUG", ">>> $buttonName CLICKED <<<")
        logNavState(buttonName)
        try {
            val navController = findNavController()
            val currentDestId = navController.currentDestination?.id
            if (currentDestId == R.id.navigation_inventory) {
                Log.d("NAV_DEBUG", "  -> GUARD PASSED, calling navigate($actionId)")
                navController.navigate(actionId)
                Log.d("NAV_DEBUG", "  -> navigate() returned successfully")
            } else {
                Log.w("NAV_DEBUG", "  -> GUARD BLOCKED: currentDest=$currentDestId, expected=${R.id.navigation_inventory}")
            }
        } catch (e: Exception) {
            Log.e("NAV_DEBUG", "  -> EXCEPTION during navigate", e)
            logNavState("$buttonName-after-exception")
        }
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

    override fun onResume() {
        super.onResume()
        if (expiringCuteAdapter.infiniteMode && expiringCuteAdapter.currentList.isNotEmpty()) {
            binding.recyclerExpiringSoon.startAutoScroll()
        }
        ScanResultHolder.result?.let { scanResult ->
            ScanResultHolder.result = null
            pendingScanImageBytes = scanResult.imageBytes
            val expiryDate = parseDate(scanResult.expiryHint)
                ?: ShelfLifeEstimator.calculateExpiryDate(
                    ShelfLifeEstimator.estimateShelfLife(listOf(scanResult.foodName.lowercase())).days
                )
            showAddEditDialog(FoodItem(
                name = scanResult.foodName,
                expiryDate = expiryDate,
                category = FoodCategory.OTHER,
                location = StorageLocation.FRIDGE,
                quantity = 1,
                dateAdded = LocalDate.now(),
                notes = "AI Scan (Confidence: ${String.format("%.0f%%", scanResult.confidence * 100)})"
            ))
        }
    }

    override fun onPause() {
        super.onPause()
        binding.recyclerExpiringSoon.stopAutoScroll()
        binding.recyclerExpiringSoon.cancelResumeTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerExpiringSoon.stopAutoScroll()
        binding.recyclerExpiringSoon.cancelResumeTimer()
        _binding = null
        currentDialog = null
    }
}
