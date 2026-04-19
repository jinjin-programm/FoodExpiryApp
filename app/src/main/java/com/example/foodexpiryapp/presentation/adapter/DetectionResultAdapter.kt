package com.example.foodexpiryapp.presentation.adapter

import android.graphics.Typeface
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import com.example.foodexpiryapp.databinding.ItemDetectionResultBinding
import java.io.File

/**
 * RecyclerView adapter for detection results in the ConfirmationFragment.
 *
 * Per D-09: Each row shows food name, confidence badge (High/Medium/Low), edit/delete icons.
 * Per D-11: Failed items show "Unknown item" gray italic, orange background, "Fix" button.
 * Per D-13: All colors extracted to colors.xml resource references.
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

            if (entity.shelfLifeSource == "auto") {
                binding.tvAiBadge.visibility = View.VISIBLE
            } else {
                binding.tvAiBadge.visibility = View.GONE
            }

            if (isFailed) {
                bindFailedItem(entity)
            } else {
                bindClassifiedItem(entity)
            }

            loadThumbnail(entity)

            // Edit button
            binding.btnEdit.setOnClickListener {
                if (isFailed) {
                    showEditDialog(entity)
                } else {
                    showEditDialog(entity)
                }
            }

            // Delete button — hidden for failed items, show Fix instead
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                onDeleteClick(entity)
            }

            if (isFailed) {
                binding.btnEdit.setIconResource(android.R.drawable.ic_menu_edit)
                binding.btnEdit.text = "Fix"
            }
        }

        private fun bindClassifiedItem(entity: DetectionResultEntity) {
            binding.tvFoodName.text = entity.foodName.ifBlank { "Unknown" }
            binding.tvFoodName.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.detection_text_primary)
            )
            binding.tvFoodName.setTypeface(null, Typeface.NORMAL)

            // Confidence badge colors: High (green, ≥0.8), Medium (yellow, ≥0.5), Low (red, <0.5)
            val confidence = entity.confidence.coerceAtLeast(entity.llmConfidence)
            when {
                confidence >= 0.8f -> {
                    binding.tvConfidenceBadge.text = "High"
                    binding.tvConfidenceBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.detection_confidence_high)
                    )
                }
                confidence >= 0.5f -> {
                    binding.tvConfidenceBadge.text = "Medium"
                    binding.tvConfidenceBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.detection_confidence_medium)
                    )
                }
                else -> {
                    binding.tvConfidenceBadge.text = "Low"
                    binding.tvConfidenceBadge.setTextColor(
                        ContextCompat.getColor(binding.root.context, R.color.detection_confidence_low)
                    )
                }
            }

            // Normal background
            binding.root.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        /**
         * Per D-11: Failed items — gray italic "Unknown item", orange/yellow background.
         */
        private fun bindFailedItem(entity: DetectionResultEntity) {
            binding.tvFoodName.text = "Unknown item"
            binding.tvFoodName.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.detection_failed_text)
            )
            binding.tvFoodName.setTypeface(null, Typeface.ITALIC)

            binding.tvConfidenceBadge.text = "Failed"
            binding.tvConfidenceBadge.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.detection_failed_badge)
            )

            // Orange/yellow tint background
            binding.root.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, R.color.detection_failed_background)
            )
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

        private fun loadThumbnail(entity: DetectionResultEntity) {
            if (!entity.cropImagePath.isNullOrBlank()) {
                val file = File(entity.cropImagePath)
                if (file.exists()) {
                    Glide.with(binding.root.context)
                        .load(file)
                        .centerCrop()
                        .into(binding.imgFoodThumbnail)
                    return
                }
            }
            binding.imgFoodThumbnail.setImageDrawable(null)
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
