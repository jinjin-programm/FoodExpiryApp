package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.databinding.ItemDetectionResultBinding

/**
 * RecyclerView adapter for detection results in the ConfirmationFragment.
 *
 * Per D-09: Each row shows food name, confidence badge (High/Medium/Low), edit/delete icons.
 * Per D-11: Failed items show "Unknown item" gray italic, orange background, "Fix" button.
 */
class DetectionResultAdapter(
    private val onEditClick: (DetectionResultEntity) -> Unit,
    private val onDeleteClick: (DetectionResultEntity) -> Unit
) : ListAdapter<DetectionResultEntity, DetectionResultAdapter.ViewHolder>(DetectionResultDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectionResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDetectionResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entity: DetectionResultEntity) {
            val isFailed = entity.status == DetectionResultEntity.STATUS_FAILED

            if (isFailed) {
                bindFailedItem(entity)
            } else {
                bindClassifiedItem(entity)
            }

            // Edit button
            binding.btnEdit.setOnClickListener {
                if (isFailed) {
                    showEditDialog(entity)
                } else {
                    showEditDialog(entity)
                }
            }

            // Delete button — hidden for failed items, show Fix instead
            if (isFailed) {
                binding.btnDelete.visibility = View.GONE
                binding.btnEdit.setIconResource(android.R.drawable.ic_menu_edit)
                // Fix button appearance for failed items
                binding.btnEdit.text = "Fix"
            } else {
                binding.btnDelete.visibility = View.VISIBLE
                binding.btnDelete.setOnClickListener {
                    onDeleteClick(entity)
                }
            }
        }

        private fun bindClassifiedItem(entity: DetectionResultEntity) {
            binding.tvFoodName.text = entity.foodName.ifBlank { "Unknown" }
            binding.tvFoodName.setTextColor(Color.parseColor("#212121"))
            binding.tvFoodName.setTypeface(null, Typeface.NORMAL)

            // Confidence badge colors: High (green, ≥0.8), Medium (yellow, ≥0.5), Low (red, <0.5)
            val confidence = entity.confidence.coerceAtLeast(entity.llmConfidence)
            when {
                confidence >= 0.8f -> {
                    binding.tvConfidenceBadge.text = "High"
                    binding.tvConfidenceBadge.setTextColor(Color.parseColor("#2E7D32"))
                }
                confidence >= 0.5f -> {
                    binding.tvConfidenceBadge.text = "Medium"
                    binding.tvConfidenceBadge.setTextColor(Color.parseColor("#F57F17"))
                }
                else -> {
                    binding.tvConfidenceBadge.text = "Low"
                    binding.tvConfidenceBadge.setTextColor(Color.parseColor("#C62828"))
                }
            }

            // Normal background
            binding.root.setBackgroundColor(Color.TRANSPARENT)
        }

        /**
         * Per D-11: Failed items — gray italic "Unknown item", orange/yellow background.
         */
        private fun bindFailedItem(entity: DetectionResultEntity) {
            binding.tvFoodName.text = "Unknown item"
            binding.tvFoodName.setTextColor(Color.parseColor("#9E9E9E"))
            binding.tvFoodName.setTypeface(null, Typeface.ITALIC)

            binding.tvConfidenceBadge.text = "Failed"
            binding.tvConfidenceBadge.setTextColor(Color.parseColor("#E65100"))

            // Orange/yellow tint background
            binding.root.setBackgroundColor(Color.parseColor("#FFF3E0"))
        }

        /**
         * Opens a simple AlertDialog with EditText for name editing.
         */
        private fun showEditDialog(entity: DetectionResultEntity) {
            val context = binding.root.context
            val editText = EditText(context).apply {
                setText(entity.foodName)
                inputType = InputType.TYPE_CLASS_TEXT
                setSelection(text.length)
                setPadding(48, 24, 48, 24)
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 24, 48, 0)
                addView(editText)
            }

            AlertDialog.Builder(context)
                .setTitle(if (entity.status == DetectionResultEntity.STATUS_FAILED) "Fix item name" else "Edit item name")
                .setView(container)
                .setPositiveButton("Save") { _, _ ->
                    val newName = editText.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        onEditClick(entity.copy(foodName = newName))
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class DetectionResultDiffCallback : DiffUtil.ItemCallback<DetectionResultEntity>() {
        override fun areItemsTheSame(
            oldItem: DetectionResultEntity,
            newItem: DetectionResultEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: DetectionResultEntity,
            newItem: DetectionResultEntity
        ): Boolean = oldItem == newItem
    }
}
