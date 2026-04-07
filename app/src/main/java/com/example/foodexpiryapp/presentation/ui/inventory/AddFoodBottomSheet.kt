package com.example.foodexpiryapp.presentation.ui.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.DialogAddFoodBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.ScanSource
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.presentation.viewmodel.InventoryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@AndroidEntryPoint
class AddFoodBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogAddFoodBinding? = null
    private val binding get() = _binding!!

    private val inventoryViewModel: InventoryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddFoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDropdowns()
        prefillData()

        binding.btnSave.setOnClickListener {
            saveFoodItem()
        }
    }

    private fun setupDropdowns() {
        val categories = FoodCategory.values().map { it.displayName }
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.dropdownCategory.setAdapter(categoryAdapter)

        val locations = StorageLocation.values().map { it.displayName }
        val locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations)
        binding.dropdownLocation.setAdapter(locationAdapter)
    }

    private fun prefillData() {
        arguments?.let { args ->
            val barcode = args.getString(ARG_BARCODE)
            val expiryDate = args.getString(ARG_EXPIRY_DATE)

            if (!barcode.isNullOrEmpty()) {
                binding.editBarcode.setText(barcode)
                // In a real app we might look up the barcode to get the product name
                binding.editFoodName.setText("Scanned Product")
            }

            if (!expiryDate.isNullOrEmpty()) {
                // Assuming expiryDate is passed in as a string
                binding.editExpiryDate.setText(expiryDate)
            } else {
                // Default to 7 days from now if no date is provided
                val defaultDate = LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                binding.editExpiryDate.setText(defaultDate)
            }
        }
    }

    private fun saveFoodItem() {
        val name = binding.editFoodName.text.toString().trim()
        val barcode = binding.editBarcode.text.toString().trim().takeIf { it.isNotEmpty() }
        val categoryStr = binding.dropdownCategory.text.toString().trim()
        val locationStr = binding.dropdownLocation.text.toString().trim()
        val expiryStr = binding.editExpiryDate.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter a food name", Toast.LENGTH_SHORT).show()
            return
        }

        if (expiryStr.isEmpty()) {
            Toast.makeText(context, "Please enter an expiry date", Toast.LENGTH_SHORT).show()
            return
        }

        val category = FoodCategory.values().find { it.displayName == categoryStr } ?: FoodCategory.OTHER
        val location = StorageLocation.values().find { it.displayName == locationStr } ?: StorageLocation.FRIDGE

        val expiryDate: LocalDate
        try {
            // Attempt to parse standard formats
            expiryDate = parseDate(expiryStr)
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid date format. Use yyyy-MM-dd", Toast.LENGTH_SHORT).show()
            return
        }

        val scanSource = if (arguments?.getString(ARG_BARCODE) != null || arguments?.getString(ARG_EXPIRY_DATE) != null) {
            ScanSource.BARCODE // Or OCR if we differentiate
        } else {
            ScanSource.MANUAL
        }

        val foodItem = FoodItem(
            name = name,
            category = category,
            location = location,
            expiryDate = expiryDate,
            barcode = barcode,
            scanSource = scanSource
        )

        inventoryViewModel.onAddFoodItem(foodItem)
        dismiss()
    }

    private fun parseDate(dateStr: String): LocalDate {
        // Try standard ISO format first
        try {
            return LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            // Try dd/MM/yyyy or MM/dd/yyyy if needed
            val formatters = listOf(
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM-dd-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
            )
            for (formatter in formatters) {
                try {
                    return LocalDate.parse(dateStr, formatter)
                } catch (ignored: DateTimeParseException) {}
            }
            throw e
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddFoodBottomSheet"
        private const val ARG_BARCODE = "arg_barcode"
        private const val ARG_EXPIRY_DATE = "arg_expiry_date"

        fun newInstance(barcode: String? = null, expiryDate: String? = null): AddFoodBottomSheet {
            val fragment = AddFoodBottomSheet()
            val args = Bundle()
            if (barcode != null) {
                args.putString(ARG_BARCODE, barcode)
            }
            if (expiryDate != null) {
                args.putString(ARG_EXPIRY_DATE, expiryDate)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
