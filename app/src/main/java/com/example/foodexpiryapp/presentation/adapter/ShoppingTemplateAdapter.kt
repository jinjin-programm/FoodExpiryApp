package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemShoppingTemplateCardBinding
import com.example.foodexpiryapp.domain.model.ShoppingTemplate

class ShoppingTemplateAdapter(
    private val onApplyTemplate: (ShoppingTemplate) -> Unit
) : ListAdapter<ShoppingTemplate, ShoppingTemplateAdapter.ViewHolder>(TemplateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingTemplateCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemShoppingTemplateCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(template: ShoppingTemplate) {
            binding.textTemplateName.text = template.name
            binding.textTemplateDescription.text = template.description
            binding.chipItemCount.text = "${template.itemNames.size} items"

            binding.root.setOnClickListener {
                onApplyTemplate(template)
            }
        }
    }

    private class TemplateDiffCallback : DiffUtil.ItemCallback<ShoppingTemplate>() {
        override fun areItemsTheSame(oldItem: ShoppingTemplate, newItem: ShoppingTemplate): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShoppingTemplate, newItem: ShoppingTemplate): Boolean = oldItem == newItem
    }
}
