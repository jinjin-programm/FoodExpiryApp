package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodexpiryapp.databinding.ItemFoodBinding
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.util.FoodImageResolver
import java.time.format.DateTimeFormatter

class FoodItemAdapter(
    private val onItemClick: (FoodItem) -> Unit,
    private val onEatenToggle: (FoodItem, Boolean) -> Unit
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

            val imageRes = FoodImageResolver.getFoodImage(item.name, item.category)
            Glide.with(binding.root.context)
                .load(imageRes)
                .centerCrop()
                .into(binding.imgFoodItem)

            // Reset checkbox state
            binding.checkboxEaten.setOnCheckedChangeListener(null)
            binding.checkboxEaten.isChecked = false

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

            binding.root.setOnClickListener { onItemClick(item) }

            binding.checkboxEaten.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    onEatenToggle(item, true)
                }
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
