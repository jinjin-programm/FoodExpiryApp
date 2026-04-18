package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemExpiringCuteBinding
import com.example.foodexpiryapp.domain.model.FoodItem

class ExpiringCuteAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, ExpiringCuteAdapter.ViewHolder>(DiffCallback()) {

    private data class CardColors(val bgColor: Int, val textColor: Int)

    private val palette = listOf(
        CardColors(Color.parseColor("#DBEAFE"), Color.parseColor("#1D4ED8")),
        CardColors(Color.parseColor("#FED7AA"), Color.parseColor("#C2410C")),
        CardColors(Color.parseColor("#FEF08A"), Color.parseColor("#A16207")),
        CardColors(Color.parseColor("#FED7AA"), Color.parseColor("#C2410C")),
        CardColors(Color.parseColor("#DBEAFE"), Color.parseColor("#1D4ED8"))
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpiringCuteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: ItemExpiringCuteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem, position: Int) {
            binding.textFoodName.text = item.name

            val days = item.daysUntilExpiry
            binding.textDaysLeft.text = when {
                days < 0 -> "Expired"
                days == 0L -> "Today"
                else -> "$days day${if (days > 1) "s" else ""} left"
            }

            val colors = palette[position % palette.size]
            binding.root.setCardBackgroundColor(colors.bgColor)
            binding.textFoodName.setTextColor(colors.textColor)
            binding.textDaysLeft.setTextColor(colors.textColor)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
    }
}
