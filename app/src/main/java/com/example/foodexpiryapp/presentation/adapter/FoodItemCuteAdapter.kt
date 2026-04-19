package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import java.io.File
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodexpiryapp.databinding.ItemFoodCuteBinding
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.util.FoodImageResolver

class FoodItemCuteAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodItemCuteAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFoodCuteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFoodCuteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem) {
            binding.textFoodName.text = item.name
            binding.textFoodSubtitle.text = item.location.displayName

            val days = item.daysUntilExpiry
            when {
                days < 0 -> {
                    binding.textStatusBadge.text = "EXPIRED"
                    binding.textStatusBadge.setTextColor(Color.parseColor("#C62828"))
                    binding.textStatusBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                days <= 3 -> {
                    binding.textStatusBadge.text = "EXPIRING IN $days day${if (days > 1) "s" else ""}"
                    binding.textStatusBadge.setTextColor(Color.parseColor("#E65100"))
                    binding.textStatusBadge.setBackgroundColor(Color.parseColor("#FFF3E0"))
                }
                else -> {
                    binding.textStatusBadge.text = "GOOD FOR $days day${if (days > 1) "s" else ""}"
                    binding.textStatusBadge.setTextColor(Color.parseColor("#2E7D32"))
                    binding.textStatusBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
                }
            }

            val glideRequest = if (!item.imagePath.isNullOrBlank()) {
                val file = File(item.imagePath)
                if (file.exists()) {
                    Glide.with(binding.root.context)
                        .load(file)
                } else {
                    Glide.with(binding.root.context)
                        .load(FoodImageResolver.getFoodImage(item.name, item.category))
                }
            } else {
                Glide.with(binding.root.context)
                    .load(FoodImageResolver.getFoodImage(item.name, item.category))
            }
            glideRequest.centerCrop().into(binding.imgFood)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
    }
}
