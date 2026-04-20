package com.example.foodexpiryapp.presentation.ui.recipes

import android.content.Intent
import android.net.Uri
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentRecipeDetailBinding
import com.example.foodexpiryapp.domain.model.RecipeIngredient
import com.example.foodexpiryapp.presentation.adapter.IngredientChecklistAdapter
import com.example.foodexpiryapp.presentation.viewmodel.RecipeDetailEvent
import com.example.foodexpiryapp.presentation.viewmodel.RecipeDetailViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecipeDetailViewModel by viewModels()
    private val args: RecipeDetailFragmentArgs by navArgs()

    private lateinit var ingredientAdapter: IngredientChecklistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeState()
        observeEvents()

        viewModel.loadRecipe(args.recipeId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        ingredientAdapter = IngredientChecklistAdapter()
        binding.recyclerIngredients.apply {
            adapter = ingredientAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupListeners() {
        binding.btnYoutube.setOnClickListener {
            val url = viewModel.uiState.value.recipe?.youtubeUrl
            if (!url.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Could not open YouTube link", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnAddToShopping.setOnClickListener {
            val selected = ingredientAdapter.getSelectedMissingIngredients()
            if (selected.isNotEmpty()) {
                viewModel.onAddSelectedToShoppingList(selected)
            } else {
                Snackbar.make(binding.root, "No missing items selected", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnCooked.setOnClickListener {
            viewModel.onCooked()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.recipe?.let { recipe ->
                        binding.imageRecipeDetail.load(recipe.imageUrl) {
                            crossfade(true)
                            placeholder(android.R.drawable.progress_horizontal)
                            error(android.R.drawable.ic_menu_report_image)
                        }
                        binding.textRecipeTitle.text = recipe.name
                        binding.textRecipeInfo.text = "Cuisine: ${recipe.cuisine} | Time: ${recipe.totalTimeMinutes} min"
                        binding.textInstructions.text = recipe.description
                        
                        val inventoryNames = state.inventoryItems.map { it.name.lowercase() }
                        ingredientAdapter.submitIngredients(recipe.ingredients, inventoryNames)

                        binding.btnYoutube.visibility = if (recipe.youtubeUrl.isNullOrBlank()) View.GONE else View.VISIBLE
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
                        is RecipeDetailEvent.ShowMessage -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is RecipeDetailEvent.NavigateBack -> {
                            findNavController().navigateUp()
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
