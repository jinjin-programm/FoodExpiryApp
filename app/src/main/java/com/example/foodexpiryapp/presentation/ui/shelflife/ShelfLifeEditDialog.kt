package com.example.foodexpiryapp.presentation.ui.shelflife

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.data.local.database.ShelfLifeEntity
import com.example.foodexpiryapp.databinding.DialogShelfLifeEditBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShelfLifeEditDialog : DialogFragment() {

    private var _binding: DialogShelfLifeEditBinding? = null
    private val binding get() = _binding!!

    private var existingEntity: ShelfLifeEntity? = null
    private var onSave: ((name: String, days: Int, category: String, location: String) -> Unit)? = null
    private var onDelete: ((id: Long) -> Unit)? = null

    fun setExistingEntity(entity: ShelfLifeEntity) {
        existingEntity = entity
    }

    fun setCallbacks(
        onSave: (name: String, days: Int, category: String, location: String) -> Unit,
        onDelete: ((id: Long) -> Unit)? = null
    ) {
        this.onSave = onSave
        this.onDelete = onDelete
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogShelfLifeEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDropdowns()

        existingEntity?.let { entity ->
            binding.editFoodName.setText(entity.foodName)
            binding.editShelfLifeDays.setText(entity.shelfLifeDays.toString())
            binding.spinnerCategory.setText(entity.category, false)
            binding.spinnerLocation.setText(entity.location, false)
        }

        val title = if (existingEntity != null) "Edit Entry" else "Add Entry"
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.editFoodName.text.toString().trim()
                val days = binding.editShelfLifeDays.text.toString().toIntOrNull() ?: 7
                val category = binding.spinnerCategory.text.toString()
                val location = binding.spinnerLocation.text.toString()
                if (name.isNotBlank()) {
                    onSave?.invoke(name, days, category, location)
                }
            }
            .setNegativeButton("Cancel", null)

        if (existingEntity != null && onDelete != null) {
            val entity = existingEntity!!
            builder.setNeutralButton("Delete") { _, _ ->
                onDelete?.invoke(entity.id)
            }
        }

        builder.create().show()
    }

    private fun setupDropdowns() {
        val categories = resources.getStringArray(R.array.food_categories)
        val locations = resources.getStringArray(R.array.storage_locations)

        binding.spinnerCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        )
        binding.spinnerLocation.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
