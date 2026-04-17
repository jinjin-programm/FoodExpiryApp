package com.example.foodexpiryapp.presentation.ui.shelflife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import com.example.foodexpiryapp.databinding.ItemShelfLifeBinding

class ShelfLifeAdapter(
    private val onConfirmClick: (ShelfLifeEntity) -> Unit,
    private val onEditClick: (ShelfLifeEntity) -> Unit
) : ListAdapter<ShelfLifeEntity, ShelfLifeAdapter.ViewHolder>(ShelfLifeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShelfLifeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemShelfLifeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: ShelfLifeEntity) {
            binding.tvFoodName.text = entity.foodName.replaceFirstChar { it.uppercase() }
            binding.tvCategoryBadge.text = entity.category
            binding.tvShelfLifeInfo.text = "${entity.shelfLifeDays} days \u2022 ${entity.location.lowercase().replaceFirstChar { it.uppercase() }}"

            if (entity.source == "auto") {
                binding.tvSourceBadge.text = "AI LEARNED"
                binding.tvSourceBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.primary_fixed))
                binding.tvSourceBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.primary))
                binding.btnConfirm.visibility = View.VISIBLE
            } else {
                binding.tvSourceBadge.text = "\u2713 CONFIRMED"
                binding.tvSourceBadge.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.secondary_container))
                binding.tvSourceBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.secondary))
                binding.btnConfirm.visibility = View.GONE
            }

            binding.btnConfirm.setOnClickListener { onConfirmClick(entity) }
            binding.btnEdit.setOnClickListener { onEditClick(entity) }
        }
    }

    class ShelfLifeDiffCallback : DiffUtil.ItemCallback<ShelfLifeEntity>() {
        override fun areItemsTheSame(oldItem: ShelfLifeEntity, newItem: ShelfLifeEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShelfLifeEntity, newItem: ShelfLifeEntity): Boolean = oldItem == newItem
    }
}
