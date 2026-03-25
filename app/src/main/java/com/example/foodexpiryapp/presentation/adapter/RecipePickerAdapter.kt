package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemRecipePickerBinding
import com.example.foodexpiryapp.domain.model.Recipe

class RecipePickerAdapter(
    private val onRecipeSelected: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipePickerAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

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

        fun bind(recipe: Recipe) {
            binding.textRecipeName.text = recipe.name

            val ingredientsText = recipe.ingredients.take(4).joinToString(", ") { it.name }
            val moreCount = recipe.ingredients.size - 4
            binding.textIngredients.text = if (moreCount > 0) {
                "$ingredientsText +$moreCount more"
            } else {
                ingredientsText
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
                onRecipeSelected(recipe)
            }
        }
    }

    private class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}
