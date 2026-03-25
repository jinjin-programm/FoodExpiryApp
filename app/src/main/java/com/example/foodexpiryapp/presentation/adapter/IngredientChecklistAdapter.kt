package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemIngredientChecklistBinding
import com.example.foodexpiryapp.domain.model.RecipeIngredient

class IngredientChecklistAdapter : RecyclerView.Adapter<IngredientChecklistAdapter.ViewHolder>() {

    private var items = emptyList<Pair<RecipeIngredient, Boolean>>()
    private val selectedMissing = mutableSetOf<String>()

    fun submitIngredients(ingredients: List<RecipeIngredient>, inventoryNames: List<String>) {
        items = ingredients.map { ing ->
            val hasItem = inventoryNames.any { it.contains(ing.name.lowercase()) || ing.name.lowercase().contains(it) }
            Pair(ing, hasItem)
        }
        notifyDataSetChanged()
    }

    fun getSelectedMissingIngredients(): List<String> = selectedMissing.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemIngredientChecklistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (ingredient, hasItem) = items[position]
        holder.bind(ingredient, hasItem)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemIngredientChecklistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ingredient: RecipeIngredient, hasItem: Boolean) {
            // Clear listener first to avoid triggering it when we change the state below
            binding.checkboxIngredient.setOnCheckedChangeListener(null)
            
            binding.textIngredientName.text = "${ingredient.name} (${ingredient.quantity})"
            
            if (hasItem) {
                binding.checkboxIngredient.isChecked = true
                binding.checkboxIngredient.isEnabled = false
                binding.textStatus.text = "✅ In Pantry"
                binding.textStatus.setTextColor(0xFF2E7D32.toInt())
            } else {
                binding.checkboxIngredient.isChecked = selectedMissing.contains(ingredient.name)
                binding.checkboxIngredient.isEnabled = true
                binding.textStatus.text = "❌ Missing"
                binding.textStatus.setTextColor(0xFFC62828.toInt())
                
                binding.checkboxIngredient.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedMissing.add(ingredient.name)
                    else selectedMissing.remove(ingredient.name)
                }
            }
        }
    }
}
