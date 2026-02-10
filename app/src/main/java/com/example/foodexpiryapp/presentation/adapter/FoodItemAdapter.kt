package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemFoodBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import java.time.format.DateTimeFormatter

class FoodItemAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodItemAdapter.FoodItemViewHolder>(FoodItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodItemViewHolder {
        val binding = ItemFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodItemViewHolder(
        private val binding: ItemFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

        fun bind(item: FoodItem) {
            binding.textFoodName.text = item.name
            binding.textCategory.text = item.category.displayName
            binding.textLocation.text = item.location.displayName
            binding.textQuantity.text = "Qty: ${item.quantity}"
            binding.textExpiryDate.text = item.expiryDate.format(dateFormatter)

            // Days left label + color
            val days = item.daysUntilExpiry
            when {
                days < 0 -> {
                    binding.textDaysLeft.text = "EXPIRED"
                    binding.textDaysLeft.setTextColor(Color.parseColor("#D32F2F"))
                    binding.textExpiryDate.setTextColor(Color.parseColor("#D32F2F"))
                }
                days == 0L -> {
                    binding.textDaysLeft.text = "Today!"
                    binding.textDaysLeft.setTextColor(Color.parseColor("#F57C00"))
                    binding.textExpiryDate.setTextColor(Color.parseColor("#F57C00"))
                }
                days <= 3 -> {
                    binding.textDaysLeft.text = "$days day${if (days > 1) "s" else ""} left"
                    binding.textDaysLeft.setTextColor(Color.parseColor("#F57C00"))
                    binding.textExpiryDate.setTextColor(Color.parseColor("#F57C00"))
                }
                else -> {
                    binding.textDaysLeft.text = "$days days left"
                    binding.textDaysLeft.setTextColor(Color.parseColor("#388E3C"))
                    binding.textExpiryDate.setTextColor(Color.parseColor("#388E3C"))
                }
            }

            // Category color indicator
            val categoryColor = getCategoryColor(item.category)
            binding.categoryIndicator.setBackgroundColor(Color.parseColor(categoryColor))

            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun getCategoryColor(category: FoodCategory): String {
            return when (category) {
                FoodCategory.DAIRY -> "#42A5F5"
                FoodCategory.MEAT -> "#EF5350"
                FoodCategory.VEGETABLES -> "#66BB6A"
                FoodCategory.FRUITS -> "#FFA726"
                FoodCategory.GRAINS -> "#8D6E63"
                FoodCategory.BEVERAGES -> "#26C6DA"
                FoodCategory.SNACKS -> "#AB47BC"
                FoodCategory.CONDIMENTS -> "#FFEE58"
                FoodCategory.FROZEN -> "#78909C"
                FoodCategory.LEFTOVERS -> "#EC407A"
                FoodCategory.OTHER -> "#BDBDBD"
            }
        }
    }
}

class FoodItemDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
    override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
        return oldItem == newItem
    }
}
