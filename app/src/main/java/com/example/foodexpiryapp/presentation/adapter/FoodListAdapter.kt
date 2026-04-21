package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import java.io.File
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodexpiryapp.databinding.ItemFoodListBinding
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.util.FoodImageResolver

class FoodListAdapter(
    private val onItemClick: (FoodItem) -> Unit,
    private var allergenItemIds: Set<Long> = emptySet()
) : ListAdapter<FoodItem, FoodListAdapter.FoodListViewHolder>(FoodListDiffCallback()) {

    fun updateAllergenItems(newAllergenItemIds: Set<Long>) {
        allergenItemIds = newAllergenItemIds
        notifyDataSetChanged()
    }

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
            
            if (allergenItemIds.contains(item.id)) {
                binding.textFoodName.setTextColor(Color.parseColor("#C62828"))
            } else {
                binding.textFoodName.setTextColor(Color.parseColor("#212121"))
            }
            
            binding.textFoodInfo.text = "Added recently • ${item.category.displayName}"
            
            val days = item.daysUntilExpiry
            binding.textExpiryDays.text = when {
                days < 0 -> "Expired"
                days == 0L -> "Today"
                else -> "$days Days"
            }

            val glideRequest = if (!item.imagePath.isNullOrBlank()) {
                val file = File(item.imagePath)
                if (file.exists()) Glide.with(binding.root.context).load(file)
                else Glide.with(binding.root.context).load(FoodImageResolver.getFoodImage(item.name, item.category))
            } else {
                Glide.with(binding.root.context).load(FoodImageResolver.getFoodImage(item.name, item.category))
            }
            glideRequest.centerCrop().into(binding.imgFood)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}

class FoodListDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
    override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
}
