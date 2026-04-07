package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemRecipePickerBinding
import com.example.foodexpiryapp.domain.model.Recipe
import com.example.foodexpiryapp.domain.model.RecipeMatch

class RecipePickerAdapter(
    private val onRecipeSelected: (RecipeMatch) -> Unit
) : ListAdapter<RecipeMatch, RecipePickerAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipePickerBinding.inflate(
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
        private val binding: ItemRecipePickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(match: RecipeMatch) {
            val recipe = match.recipe

            binding.textRecipeName.text = recipe.name

            val ingredientsText = recipe.ingredients.take(3).joinToString(", ") { it.name }
            binding.textIngredients.text = ingredientsText

            if (match.matchCount > 0) {
                binding.textMatchInfo.text = "${match.matchCount} ingredients rescued"
                binding.textMatchInfo.visibility = View.VISIBLE
            } else {
                binding.textMatchInfo.visibility = View.GONE
            }

            val emoji = when {
                recipe.tags.any { it.name.contains("PASTA") } -> "🍝"
                recipe.tags.any { it.name.contains("SALAD") } -> "🥗"
                recipe.tags.any { it.name.contains("SOUP") } -> "🍲"
                recipe.tags.any { it.name.contains("BREAKFAST") } -> "🍳"
                recipe.tags.any { it.name.contains("DESSERT") } -> "🍰"
                recipe.tags.any { it.name.contains("SMOOTHIE") } -> "🥤"
                recipe.tags.any { it.name.contains("RICE") } -> "🍚"
                recipe.tags.any { it.name.contains("BREAD") } -> "🍞"
                else -> "🍽️"
            }
            binding.textEmoji.text = emoji

            binding.root.setOnClickListener {
                onRecipeSelected(match)
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
