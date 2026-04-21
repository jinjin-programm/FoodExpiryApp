package com.example.foodexpiryapp.presentation.ui.recipes

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.DialogAddRecipeBinding
import com.example.foodexpiryapp.databinding.FragmentRecipesBinding
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.RecipeAdapter
import com.example.foodexpiryapp.presentation.viewmodel.RecipeFilter
import com.example.foodexpiryapp.presentation.viewmodel.RecipesEvent
import com.example.foodexpiryapp.presentation.viewmodel.RecipesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecipesFragment : Fragment() {

    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipesViewModel by viewModels()

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private lateinit var recipeAdapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilterChips()
        setupFab()
        observeState()
        observeEvents()
    }

    private fun setupFab() {
        binding.fabAddRecipe.setOnClickListener {
            showAddRecipeDialog()
        }
    }

    private fun showAddRecipeDialog() {
        val dialogBinding = DialogAddRecipeBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Custom Recipe")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.editRecipeName.text.toString()
                val description = dialogBinding.editRecipeDescription.text.toString()
                val ingredients = dialogBinding.editRecipeIngredients.text.toString()
                val steps = dialogBinding.editRecipeSteps.text.toString()
                val prepTime = dialogBinding.editPrepTime.text.toString().toIntOrNull() ?: 15
                val cookTime = dialogBinding.editCookTime.text.toString().toIntOrNull() ?: 30
                val cuisine = dialogBinding.editCuisine.text.toString()
                val imageUrl = dialogBinding.editRecipeImageUrl.text.toString().takeIf { it.isNotBlank() }

                if (name.isBlank()) {
                    Snackbar.make(binding.root, "Recipe name is required", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.onAddRecipeRequested(
                    name = name,
                    description = description,
                    ingredientsRaw = ingredients,
                    stepsRaw = steps,
                    prepTime = prepTime,
                    cookTime = cookTime,
                    cuisine = cuisine,
                    imageUrl = imageUrl
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        analyticsRepository.trackEvent(
            AnalyticsEvent(
                eventName = "screen_view",
                eventType = EventType.SCREEN_VIEW,
                additionalData = mapOf("screen_name" to "recipes")
            )
        )
    }

    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(
            onRecipeClick = { recipe ->
                val bundle = bundleOf("recipeId" to recipe.id)
                findNavController().navigate(R.id.action_inventory_to_recipe_detail, bundle)
            },
            onRecipeCooked = { match ->
                viewModel.onRecipeCooked(match.recipe, match.matchedInventoryItems)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.recipesRecyclerView.scrollToPosition(0)
                }, 300)
            }
        )

        binding.recipesRecyclerView.apply {
            adapter = recipeAdapter
            layoutManager = LinearLayoutManager(requireContext())
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    
                    if (totalItemCount <= lastVisibleItem + 2) {
                        viewModel.onLoadMoreRequested()
                    }
                }
            })
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val filter = when (checkedIds.first()) {
                binding.chipAll.id -> RecipeFilter.ALL
                binding.chipBestMatch.id -> RecipeFilter.BEST_MATCH
                binding.chipUseSoon.id -> RecipeFilter.USE_SOON
                binding.chipQuick.id -> RecipeFilter.QUICK
                binding.chipStirFry.id -> RecipeFilter.STIRFRY
                binding.chipSteamed.id -> RecipeFilter.STEAMED
                binding.chipSoup.id -> RecipeFilter.SOUP
                binding.chipRiceNoodle.id -> RecipeFilter.RICE_NOODLE
                binding.chipBraised.id -> RecipeFilter.BRAISED
                binding.chipPanFried.id -> RecipeFilter.PAN_FRIED
                binding.chipColdDish.id -> RecipeFilter.COLD_DISH
                binding.chipVegetarian.id -> RecipeFilter.VEGETARIAN
                binding.chipVegan.id -> RecipeFilter.VEGAN
                binding.chipChaChaanTeng.id -> RecipeFilter.CHA_CHAAN_TENG
                binding.chipHongKong.id -> RecipeFilter.HONG_KONG
                binding.chipChinese.id -> RecipeFilter.CHINESE
                binding.chipJapanese.id -> RecipeFilter.JAPANESE
                binding.chipThai.id -> RecipeFilter.THAI
                binding.chipKorean.id -> RecipeFilter.KOREAN
                else -> RecipeFilter.ALL
            }
            viewModel.onFilterChanged(filter)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingProgress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.recipesRecyclerView.visibility =
                        if (!state.isLoading && state.recipeMatches.isNotEmpty()) View.VISIBLE else View.GONE

                    binding.emptyStateLayout.visibility =
                        if (!state.isLoading && state.recipeMatches.isEmpty()) View.VISIBLE else View.GONE

                    binding.textMoneySaved.text = "$${String.format("%.2f", state.totalMoneySaved)}"
                    binding.textWasteRescued.text = "${state.totalWasteRescued}%"
                    binding.textRecipesUsed.text = state.recipesUsedCount.toString()

                    val expiringText = state.expiringItems.take(5).joinToString(", ") { item ->
                        "${item.name} (${item.daysUntilExpiry.toInt()} day${if (item.daysUntilExpiry.toInt() != 1) "s" else ""})"
                    }
                    binding.textExpiringItems.text = if (expiringText.isNotEmpty()) expiringText else "No items expiring soon"
                    binding.cardExpiringSoon.visibility =
                        if (state.expiringItems.isNotEmpty()) View.VISIBLE else View.GONE

                    recipeAdapter.submitList(state.recipeMatches)

                    updateSelectedFilterChip(state.selectedFilter)
                }
            }
        }
    }

    private fun updateSelectedFilterChip(filter: RecipeFilter) {
        val chipToSelect = when (filter) {
            RecipeFilter.ALL -> binding.chipAll
            RecipeFilter.BEST_MATCH -> binding.chipBestMatch
            RecipeFilter.USE_SOON -> binding.chipUseSoon
            RecipeFilter.QUICK -> binding.chipQuick
            RecipeFilter.STIRFRY -> binding.chipStirFry
            RecipeFilter.STEAMED -> binding.chipSteamed
            RecipeFilter.SOUP -> binding.chipSoup
            RecipeFilter.RICE_NOODLE -> binding.chipRiceNoodle
            RecipeFilter.BRAISED -> binding.chipBraised
            RecipeFilter.PAN_FRIED -> binding.chipPanFried
            RecipeFilter.COLD_DISH -> binding.chipColdDish
            RecipeFilter.VEGETARIAN -> binding.chipVegetarian
            RecipeFilter.VEGAN -> binding.chipVegan
            RecipeFilter.CHA_CHAAN_TENG -> binding.chipChaChaanTeng
            RecipeFilter.HONG_KONG -> binding.chipHongKong
            RecipeFilter.CHINESE -> binding.chipChinese
            RecipeFilter.JAPANESE -> binding.chipJapanese
            RecipeFilter.THAI -> binding.chipThai
            RecipeFilter.KOREAN -> binding.chipKorean
        }
        if (!chipToSelect.isChecked) {
            chipToSelect.isChecked = true
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RecipesEvent.ShowMessage -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is RecipesEvent.RecipeViewed -> {
                        }
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