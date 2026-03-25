package com.example.foodexpiryapp.presentation.ui.recipes

import android.os.Bundle
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
import com.example.foodexpiryapp.databinding.FragmentRecipesBinding
import com.example.foodexpiryapp.domain.model.AnalyticsEvent
import com.example.foodexpiryapp.domain.model.EventType
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import com.example.foodexpiryapp.presentation.adapter.RecipeAdapter
import com.example.foodexpiryapp.presentation.viewmodel.RecipeFilter
import com.example.foodexpiryapp.presentation.viewmodel.RecipesEvent
import com.example.foodexpiryapp.presentation.viewmodel.RecipesViewModel
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
        observeState()
        observeEvents()
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
                // Use the destination ID directly to avoid ViewPager navigation conflicts
                val bundle = bundleOf("recipeId" to recipe.id)
                findNavController().navigate(R.id.recipeDetailFragment, bundle)
            },
            onRecipeCooked = { match ->
                viewModel.onRecipeCooked(match.recipe, match.matchedInventoryItems)
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
                binding.chipWasteBuster.id -> RecipeFilter.WASTE_BUSTER
                binding.chipQuick.id -> RecipeFilter.QUICK
                binding.chipVegetarian.id -> RecipeFilter.VEGETARIAN
                binding.chipVegan.id -> RecipeFilter.VEGAN
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
            RecipeFilter.WASTE_BUSTER -> binding.chipWasteBuster
            RecipeFilter.QUICK -> binding.chipQuick
            RecipeFilter.VEGETARIAN -> binding.chipVegetarian
            RecipeFilter.VEGAN -> binding.chipVegan
            RecipeFilter.DAIRY_FREE -> binding.chipVegetarian
            RecipeFilter.GLUTEN_FREE -> binding.chipVegetarian
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