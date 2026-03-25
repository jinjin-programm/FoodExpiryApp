package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.foodexpiryapp.databinding.ItemRecipeBinding
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeMatch
import com.example.foodexpiryapp.domain.model.RecipeTag

class RecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit,
    private val onRecipeCooked: (RecipeMatch) -> Unit
) : ListAdapter<RecipeMatch, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecipeViewHolder(
        private val binding: ItemRecipeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: RecipeMatch) {
            val recipe = match.recipe

            binding.imageRecipe.load(recipe.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.progress_horizontal)
            }

            binding.textRecipeName.text = recipe.name
            binding.textRecipeDescription.text = recipe.description
            binding.textTime.text = "${recipe.totalTimeMinutes} min"
            binding.textMoneySaved.text = "Save ~$${String.format("%.2f", match.estimatedMoneySaved)}"
            binding.textWasteScore.text = "${recipe.wasteRescueScore}% waste rescue"

            val matchedNames = match.matchedIngredients.joinToString(", ") { it.name }
            binding.textMatchedIngredients.text = if (matchedNames.isNotEmpty()) {
                "✅ Matches: $matchedNames"
            } else {
                ""
            }

            binding.textMatchCount.text = "${match.matchCount} item${if (match.matchCount != 1) "s" else ""} matched"

            binding.textWasteBusterBadge.visibility = if (recipe.tags.contains(RecipeTag.WASTE_BUSTER)) View.VISIBLE else View.GONE
            binding.textQuickBadge.visibility = if (recipe.tags.contains(RecipeTag.QUICK)) View.VISIBLE else View.GONE
            binding.textUrgentBadge.visibility = if (recipe.tags.contains(RecipeTag.URGENT)) View.VISIBLE else View.GONE

            binding.badgeMatch.setCardBackgroundColor(
                when {
                    match.matchCount >= 4 -> 0xFFE8F5E9.toInt()
                    match.matchCount >= 2 -> 0xFFFFF3E0.toInt()
                    else -> 0xFFF3F3F3.toInt()
                }
            )

            binding.textMatchCount.setTextColor(
                when {
                    match.matchCount >= 4 -> 0xFF2E7D32.toInt()
                    match.matchCount >= 2 -> 0xFFE65100.toInt()
                    else -> 0xFF757575.toInt()
                }
            )

            binding.buttonCook.setOnClickListener {
                onRecipeCooked(match)
            }

            binding.root.setOnClickListener {
                onRecipeClick(recipe)
            }
        }
    }

    private class RecipeDiffCallback : DiffUtil.ItemCallback<RecipeMatch>() {
        override fun areItemsTheSame(oldItem: RecipeMatch, newItem: RecipeMatch): Boolean {
            return oldItem.recipe.id == newItem.recipe.id
        }

        override fun areContentsTheSame(oldItem: RecipeMatch, newItem: RecipeMatch): Boolean {
            return oldItem == newItem
        }
    }
}