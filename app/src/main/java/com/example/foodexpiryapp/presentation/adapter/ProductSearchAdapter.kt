package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemProductSearchBinding
import com.example.foodexpiryapp.domain.model.FoodItem

class ProductSearchAdapter(
    private val onProductSelected: (FoodItem) -> Unit
) : ListAdapter<FoodItem, ProductSearchAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FoodItem) {
            binding.textProductName.text = item.name

            val emoji = getCategoryEmoji(item.category.name)
            binding.textProductEmoji.text = emoji

            val daysUntilExpiry = item.daysUntilExpiry
            when {
                item.isExpired -> {
                    binding.textExpiryInfo.text = "Expired"
                    binding.textExpiryInfo.setTextColor(Color.RED)
                }
                daysUntilExpiry == 0L -> {
                    binding.textExpiryInfo.text = "Expires today"
                    binding.textExpiryInfo.setTextColor(Color.parseColor("#FF5722"))
                }
                daysUntilExpiry == 1L -> {
                    binding.textExpiryInfo.text = "Expires tomorrow"
                    binding.textExpiryInfo.setTextColor(Color.parseColor("#FF9800"))
                }
                daysUntilExpiry <= 3 -> {
                    binding.textExpiryInfo.text = "Expires in $daysUntilExpiry days"
                    binding.textExpiryInfo.setTextColor(Color.parseColor("#FF9800"))
                }
                else -> {
                    binding.textExpiryInfo.text = "Expires in $daysUntilExpiry days"
                    binding.textExpiryInfo.setTextColor(Color.parseColor("#4CAF50"))
                }
            }

            binding.root.setOnClickListener {
                onProductSelected(item)
            }
        }

        private fun getCategoryEmoji(category: String): String {
            return when (category.uppercase()) {
                "DAIRY" -> "🥛"
                "FRUITS", "FRUIT" -> "🍎"
                "VEGETABLES", "VEGETABLE" -> "🥬"
                "MEAT" -> "🥩"
                "SEAFOOD" -> "🐟"
                "GRAINS", "GRAIN" -> "🌾"
                "BEVERAGES", "BEVERAGE" -> "🥤"
                "CONDIMENTS", "CONDIMENT" -> "🧂"
                "SNACKS", "SNACK" -> "🍿"
                "FROZEN" -> "🧊"
                "BAKERY" -> "🍞"
                "CANNED" -> "🥫"
                "SPICES" -> "🌶️"
                "EGGS" -> "🥚"
                else -> "📦"
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean {
            return oldItem == newItem
        }
    }
}
