package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemFoodCardBinding
import com.example.foodexpiryapp.domain.model.FoodItem

class FoodCardAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodCardAdapter.FoodCardViewHolder>(FoodCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodCardViewHolder {
        val binding = ItemFoodCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodCardViewHolder(
        private val binding: ItemFoodCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem) {
            binding.textFoodName.text = item.name
            binding.textFoodInfo.text = "${item.category.displayName} • ${item.location.displayName}"
            
            val days = item.daysUntilExpiry
            binding.textDaysBadge.text = when {
                days < 0 -> "EXPIRED"
                days == 0L -> "TODAY"
                else -> "$days DAY${if (days > 1) "S" else ""} LEFT"
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

class FoodCardDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
    override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
}
