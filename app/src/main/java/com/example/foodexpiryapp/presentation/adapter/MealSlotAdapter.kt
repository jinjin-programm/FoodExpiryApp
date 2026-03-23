package com.example.foodexpiryapp.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.databinding.ItemMealSlotBinding
import com.example.foodexpiryapp.domain.model.MealItemType
import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot

data class MealSlotDisplayItem(
    val slot: MealSlot,
    val mealPlan: MealPlan?
)

class MealSlotAdapter(
    private val onAddRecipeClicked: (MealSlot) -> Unit,
    private val onAddProductClicked: (MealSlot) -> Unit,
    private val onDeleteClicked: (MealSlot) -> Unit,
    private val onEditClicked: (MealSlot) -> Unit
) : ListAdapter<MealSlotDisplayItem, MealSlotAdapter.MealSlotViewHolder>(MealSlotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealSlotViewHolder {
        val binding = ItemMealSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealSlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealSlotViewHolder(private val binding: ItemMealSlotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MealSlotDisplayItem) {
            val slot = item.slot
            val mealPlan = item.mealPlan

            binding.textSlotEmoji.text = slot.emoji
            binding.textSlotName.text = slot.displayName

            if (mealPlan != null) {
                binding.mealContentLayout.visibility = View.VISIBLE
                binding.emptyMealLayout.visibility = View.GONE
                binding.textMealName.text = mealPlan.displayName
                binding.textMealType.text = when (mealPlan.itemType) {
                    MealItemType.RECIPE -> "Recipe"
                    MealItemType.PRODUCT -> "Product"
                }
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnEdit.visibility = View.VISIBLE
            } else {
                binding.mealContentLayout.visibility = View.GONE
                binding.emptyMealLayout.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.GONE
                binding.btnEdit.visibility = View.GONE
            }

            binding.btnAddRecipe.setOnClickListener {
                onAddRecipeClicked(slot)
            }

            binding.btnAddProduct.setOnClickListener {
                onAddProductClicked(slot)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClicked(slot)
            }

            binding.btnEdit.setOnClickListener {
                onEditClicked(slot)
            }
        }
    }

    class MealSlotDiffCallback : DiffUtil.ItemCallback<MealSlotDisplayItem>() {
        override fun areItemsTheSame(oldItem: MealSlotDisplayItem, newItem: MealSlotDisplayItem): Boolean {
            return oldItem.slot == newItem.slot
        }

        override fun areContentsTheSame(oldItem: MealSlotDisplayItem, newItem: MealSlotDisplayItem): Boolean {
            return oldItem == newItem
        }
    }
}
