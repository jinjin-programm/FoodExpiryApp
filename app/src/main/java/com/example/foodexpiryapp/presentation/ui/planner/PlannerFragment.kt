package com.example.foodexpiryapp.presentation.ui.planner

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foodexpiryapp.databinding.DialogProductPickerBinding
import com.example.foodexpiryapp.databinding.DialogRecipePickerBinding
import com.example.foodexpiryapp.databinding.FragmentPlannerBinding
import com.example.foodexpiryapp.databinding.ItemMealSlotBinding
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeMatch
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.MealSlotDisplayItem
import com.example.foodexpiryapp.presentation.adapter.ProductSearchAdapter
import com.example.foodexpiryapp.presentation.adapter.RecipePickerAdapter
import com.example.foodexpiryapp.presentation.viewmodel.PlannerEvent
import com.example.foodexpiryapp.presentation.viewmodel.PlannerViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PlannerFragment : Fragment() {

    private var _binding: FragmentPlannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlannerViewModel by viewModels()

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private var selectedSlot: MealSlot? = null
    private var recipePickerDialog: BottomSheetDialog? = null
    private var productPickerDialog: BottomSheetDialog? = null
    private var recipePickerAdapter: RecipePickerAdapter? = null
    private var productSearchAdapter: ProductSearchAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDateSelector()
        setupSearchBar()
        setupMealSlots()
        observeState()
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        analyticsRepository.trackEvent(
            AnalyticsEvent(
                eventName = "screen_view",
                eventType = EventType.SCREEN_VIEW,
                additionalData = mapOf("screen_name" to "planner")
            )
        )
    }

    private fun setupDateSelector() {
        updateDateDisplay(LocalDate.now())

        binding.btnDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val selectedDate = selection.let {
                    LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                }
                viewModel.onDateSelected(selectedDate)
                updateDateDisplay(selectedDate)
            }

            datePicker.show(parentFragmentManager, "date_picker")
        }
    }

    private fun updateDateDisplay(date: LocalDate) {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val dateString = formatter.format(
            Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        )
        binding.textDate.text = dateString
    }

    private fun setupSearchBar() {
        binding.searchInput.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }
    }

    private fun setupMealSlots() {
        val container = binding.mealSlotsContainer
        container.removeAllViews()

        MealSlot.entries.forEach { slot ->
            val slotBinding = ItemMealSlotBinding.inflate(layoutInflater, container, false)
            slotBinding.root.tag = slot
            container.addView(slotBinding.root)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingProgress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    updateMealSlots(state.mealPlans)
                }
            }
        }
    }

    private fun updateMealSlots(mealPlans: Map<MealSlot, MealPlan?>) {
        val container = binding.mealSlotsContainer

        MealSlot.entries.forEach { slot ->
            val slotBinding = container.findViewWithTag<View>(slot)?.let {
                ItemMealSlotBinding.bind(it)
            } ?: return@forEach

            val mealPlan = mealPlans[slot]

            slotBinding.textSlotEmoji.text = slot.emoji
            slotBinding.textSlotName.text = slot.displayName

            if (mealPlan != null) {
                slotBinding.mealContentLayout.visibility = View.VISIBLE
                slotBinding.emptyMealLayout.visibility = View.GONE
                slotBinding.textMealName.text = mealPlan.displayName
                slotBinding.textMealType.text = if (mealPlan.recipeId != null) "Recipe" else "Product"
                slotBinding.btnDelete.visibility = View.VISIBLE
                slotBinding.btnEdit.visibility = View.VISIBLE
            } else {
                slotBinding.mealContentLayout.visibility = View.GONE
                slotBinding.emptyMealLayout.visibility = View.VISIBLE
                slotBinding.btnDelete.visibility = View.GONE
                slotBinding.btnEdit.visibility = View.GONE
            }

            slotBinding.btnAddRecipe.setOnClickListener {
                selectedSlot = slot
                showRecipePicker(slot)
            }

            slotBinding.btnAddProduct.setOnClickListener {
                selectedSlot = slot
                showProductPicker(slot)
            }

            slotBinding.btnDelete.setOnClickListener {
                viewModel.onDeleteMeal(slot)
            }

            slotBinding.btnEdit.setOnClickListener {
                selectedSlot = slot
                if (mealPlan?.recipeId != null) {
                    showRecipePicker(slot)
                } else {
                    showProductPicker(slot)
                }
            }
        }
    }

    private fun showRecipePicker(slot: MealSlot) {
        val dialogBinding = DialogRecipePickerBinding.inflate(layoutInflater)

        recipePickerAdapter = RecipePickerAdapter { match ->
            viewModel.onAddRecipe(slot, match.recipe)
            recipePickerDialog?.dismiss()
        }

        dialogBinding.recipesRecyclerView.apply {
            adapter = recipePickerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val suggestedRecipes = viewModel.getFilteredRecipesWithMatchInfo("")
        if (suggestedRecipes.isNotEmpty()) {
            recipePickerAdapter?.submitList(suggestedRecipes)
        } else {
            val allRecipes = viewModel.getFilteredRecipes("")
            val fallbackMatches = allRecipes.map { recipe ->
                RecipeMatch(
                    recipe = recipe,
                    matchCount = 0,
                    matchedIngredients = emptyList(),
                    matchedInventoryItems = emptyList(),
                    estimatedMoneySaved = 0.0,
                    wasteRescuePercent = 0,
                    urgencyLevel = 0,
                    dietaryFlags = emptyList()
                )
            }
            recipePickerAdapter?.submitList(fallbackMatches)
        }

        dialogBinding.searchInput.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString() ?: ""
            val suggested = viewModel.getFilteredRecipesWithMatchInfo(query)
            if (suggested.isNotEmpty()) {
                recipePickerAdapter?.submitList(suggested)
                dialogBinding.emptyStateLayout.visibility =
                    if (suggested.isEmpty()) View.VISIBLE else View.GONE
            } else {
                val allFiltered = viewModel.getFilteredRecipes(query)
                val fallbackMatches = allFiltered.map { recipe ->
                    RecipeMatch(
                        recipe = recipe,
                        matchCount = 0,
                        matchedIngredients = emptyList(),
                        matchedInventoryItems = emptyList(),
                        estimatedMoneySaved = 0.0,
                        wasteRescuePercent = 0,
                        urgencyLevel = 0,
                        dietaryFlags = emptyList()
                    )
                }
                recipePickerAdapter?.submitList(fallbackMatches)
                dialogBinding.emptyStateLayout.visibility =
                    if (fallbackMatches.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        dialogBinding.btnClose.setOnClickListener {
            recipePickerDialog?.dismiss()
        }

        recipePickerDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            setOnDismissListener {
                recipePickerAdapter = null
            }
        }
        recipePickerDialog?.show()
    }

    private fun showProductPicker(slot: MealSlot) {
        val dialogBinding = DialogProductPickerBinding.inflate(layoutInflater)

        productSearchAdapter = ProductSearchAdapter { product ->
            viewModel.onAddProduct(slot, product)
            productPickerDialog?.dismiss()
        }

        dialogBinding.inventoryRecyclerView.apply {
            adapter = productSearchAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        val inventoryItems = viewModel.uiState.value.inventoryItems
        productSearchAdapter?.submitList(inventoryItems)

        dialogBinding.searchInput.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString() ?: ""
            val filtered = viewModel.getFilteredProducts(query)
            productSearchAdapter?.submitList(filtered)
            dialogBinding.emptyInventoryLayout.visibility =
                if (filtered.isEmpty() && query.isNotEmpty()) View.VISIBLE else View.GONE
        }

        dialogBinding.btnClose.setOnClickListener {
            productPickerDialog?.dismiss()
        }

        dialogBinding.btnAddCustom.setOnClickListener {
            val customName = dialogBinding.customInput.text?.toString()?.trim()
            if (!customName.isNullOrEmpty()) {
                viewModel.onAddCustomProduct(slot, customName)
                productPickerDialog?.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter an ingredient name", Toast.LENGTH_SHORT).show()
            }
        }

        productPickerDialog = BottomSheetDialog(requireContext()).apply {
            setContentView(dialogBinding.root)
            setOnDismissListener {
                productSearchAdapter = null
            }
        }
        productPickerDialog?.show()
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PlannerEvent.ShowMessage -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is PlannerEvent.MealAdded -> {
                            val slotName = event.slot.displayName
                            Snackbar.make(
                                binding.root,
                                "Added $slotName: ${event.name}",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        is PlannerEvent.MealDeleted -> {
                            Snackbar.make(
                                binding.root,
                                "${event.slot.displayName} removed",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recipePickerDialog?.dismiss()
        productPickerDialog?.dismiss()
        _binding = null
    }
}
