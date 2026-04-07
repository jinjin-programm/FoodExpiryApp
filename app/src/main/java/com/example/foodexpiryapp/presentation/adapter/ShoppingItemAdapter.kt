package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemShoppingBinding
import com.example.foodexpiryapp.domain.model.ShoppingItem

class ShoppingItemAdapter(
    private val onToggle: (ShoppingItem) -> Unit,
    private val onDelete: (ShoppingItem) -> Unit
) : ListAdapter<ShoppingItem, ShoppingItemAdapter.ViewHolder>(ShoppingDiffCallback()) {

    private var inventoryItemNames: Set<String> = emptySet()

    fun updateInventoryStatus(names: Set<String>) {
        inventoryItemNames = names
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShoppingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemShoppingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ShoppingItem) {
            binding.checkboxShopping.isChecked = item.isChecked
            binding.textItemName.text = item.name

            val isInInventory = item.name.lowercase().trim() in inventoryItemNames.map { it.lowercase().trim() }
            binding.textInventoryStatus.visibility = if (isInInventory && !item.isChecked) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            if (item.isChecked) {
                binding.textItemName.paintFlags = binding.textItemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textItemName.alpha = 0.5f
            } else {
                binding.textItemName.paintFlags = binding.textItemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textItemName.alpha = 1.0f
            }

            binding.checkboxShopping.setOnClickListener {
                onToggle(item)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }

    private class ShoppingDiffCallback : DiffUtil.ItemCallback<ShoppingItem>() {
        override fun areItemsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShoppingItem, newItem: ShoppingItem): Boolean = oldItem == newItem
    }
}
