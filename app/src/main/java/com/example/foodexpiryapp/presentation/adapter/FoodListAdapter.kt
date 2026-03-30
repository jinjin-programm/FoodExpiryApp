package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemFoodListBinding
import com.example.foodexpiryapp.domain.model.FoodItem

class FoodListAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodListAdapter.FoodListViewHolder>(FoodListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodListViewHolder {
        val binding = ItemFoodListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodListViewHolder(
        private val binding: ItemFoodListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem) {
            binding.textFoodName.text = item.name
            binding.textFoodInfo.text = "Added recently • ${item.category.displayName}"
            
            val days = item.daysUntilExpiry
            binding.textExpiryDays.text = when {
                days < 0 -> "Expired"
                days == 0L -> "Today"
                else -> "$days Days"
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

class FoodListDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
    override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
}
