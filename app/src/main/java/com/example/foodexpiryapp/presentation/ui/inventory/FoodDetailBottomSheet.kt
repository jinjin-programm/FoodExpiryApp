package com.example.foodexpiryapp.presentation.ui.inventory

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.BottomSheetFoodDetailBinding
import com.example.foodexpiryapp.domain.model.FoodCategory
import com.example.foodexpiryapp.domain.model.FoodItem
import com.example.foodexpiryapp.domain.model.RiskLevel
import com.example.foodexpiryapp.domain.model.StorageLocation
import com.example.foodexpiryapp.presentation.viewmodel.InventoryViewModel
import com.example.foodexpiryapp.util.FoodImageResolver
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

@AndroidEntryPoint
class FoodDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFoodDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryViewModel by viewModels({ requireParentFragment() })

    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val shortFormatter = DateTimeFormatter.ofPattern("MMM d")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFoodDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.parent as? View)?.let { sheet ->
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
            behavior.skipCollapsed = true
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        val item = arguments?.let { args ->
            val id = args.getLong(ARG_ITEM_ID)
            val name = args.getString(ARG_ITEM_NAME) ?: ""
            val category = FoodCategory.values().find { it.name == args.getString(ARG_ITEM_CATEGORY) } ?: FoodCategory.OTHER
            val expiryDate = try { LocalDate.parse(args.getString(ARG_ITEM_EXPIRY_DATE) ?: "") } catch (_: Exception) { LocalDate.now() }
            val quantity = args.getInt(ARG_ITEM_QUANTITY, 1)
            val location = StorageLocation.values().find { it.name == args.getString(ARG_ITEM_LOCATION) } ?: StorageLocation.FRIDGE
            val notes = args.getString(ARG_ITEM_NOTES) ?: ""
            val riskLevel = RiskLevel.values().find { it.name == args.getString(ARG_ITEM_RISK) } ?: RiskLevel.LOW
            val imagePath = args.getString(ARG_ITEM_IMAGE_PATH)
            FoodItem(
                id = id, name = name, category = category, expiryDate = expiryDate,
                quantity = quantity, location = location, notes = notes,
                riskLevel = riskLevel, imagePath = imagePath
            )
        } ?: return

        populateUi(item)
        setupClickListeners(item)
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet = (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)
            ?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            for (i in 0 until sheet.childCount) {
                val child = sheet.getChildAt(i)
                if (child !is NestedScrollView) {
                    child.visibility = View.GONE
                }
            }
        }
    }

    private fun populateUi(item: FoodItem) {
        binding.textFoodName.text = item.name
        binding.textFoodCategory.text = item.category.displayName

        val imageRes = FoodImageResolver.getFoodImage(item.name, item.category)
        Glide.with(this).load(imageRes).centerCrop().into(binding.imgFoodIcon)

        val days = item.daysUntilExpiry
        when {
            item.isExpired -> {
                binding.textExpiryBadge.text = "Expired ${-days} day${if (-days != 1L) "s" else ""} ago"
                binding.textExpiryBadge.setTextColor(getResources().getColor(R.color.error, null))
            }
            days == 0L -> {
                binding.textExpiryBadge.text = "Expires today!"
                binding.textExpiryBadge.setTextColor(getResources().getColor(R.color.error, null))
            }
            days == 1L -> {
                binding.textExpiryBadge.text = "Expires tomorrow"
                binding.textExpiryBadge.setTextColor(getResources().getColor(R.color.error, null))
            }
            days <= 3 -> {
                binding.textExpiryBadge.text = "Expires in $days days"
                binding.textExpiryBadge.setTextColor(getResources().getColor(R.color.orange, null))
            }
            else -> {
                binding.textExpiryBadge.text = "Expires in $days days"
                binding.textExpiryBadge.setTextColor(getResources().getColor(R.color.green_500, null))
            }
        }

        binding.textExpiryDate.text = item.expiryDate.format(shortFormatter)
        binding.textDaysLeft.text = if (item.isExpired) "Expired" else days.toString()
        binding.textQuantity.text = "x${item.quantity}"

        val freshnessPercent = calculateFreshness(item)
        binding.progressFreshness.progress = freshnessPercent
        binding.textFreshnessPercent.text = "$freshnessPercent%"
        when {
            freshnessPercent <= 20 -> binding.textFreshnessPercent.setTextColor(getResources().getColor(R.color.error, null))
            freshnessPercent <= 50 -> binding.textFreshnessPercent.setTextColor(getResources().getColor(R.color.orange, null))
            else -> binding.textFreshnessPercent.setTextColor(getResources().getColor(R.color.green_500, null))
        }

        binding.chipLocation.text = "📍 ${item.location.displayName}"
        when (item.riskLevel) {
            RiskLevel.HIGH -> binding.chipRisk.text = "⚠ High Risk"
            RiskLevel.MEDIUM -> binding.chipRisk.text = "⚠ Medium"
            RiskLevel.LOW -> binding.chipRisk.text = "✓ Low Risk"
        }
        binding.chipAddedDate.text = "Added ${item.dateAdded.format(shortFormatter)}"

        if (item.notes.isNotBlank()) {
            binding.layoutNotes.visibility = View.VISIBLE
            binding.textNotes.text = item.notes
        } else {
            binding.layoutNotes.visibility = View.GONE
        }
    }

    private fun calculateFreshness(item: FoodItem): Int {
        if (item.isExpired) return 0
        val totalDays = ChronoUnit.DAYS.between(item.dateAdded, item.expiryDate).coerceAtLeast(1)
        val daysLeft = item.daysUntilExpiry.coerceAtLeast(0)
        return ((daysLeft.toDouble() / totalDays.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun setupClickListeners(item: FoodItem) {
        binding.cardRecipes.setOnClickListener {
            dismiss()
            val parentFragment = parentFragment
            if (parentFragment is InventoryFragment) {
                parentFragment.navigateToRecipes(item.name)
            }
        }

        binding.cardShopping.setOnClickListener {
            viewModel.onMarkAsEaten(item)
            Toast.makeText(requireContext(), "${item.name} — shopping feature coming soon", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.cardPlanner.setOnClickListener {
            Toast.makeText(requireContext(), "${item.name} — planner feature coming soon", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.cardMarkUsed.setOnClickListener {
            viewModel.onMarkAsEaten(item)
            dismiss()
        }

        binding.cardEdit.setOnClickListener {
            dismiss()
            val parentFragment = parentFragment
            if (parentFragment is InventoryFragment) {
                parentFragment.showAddEditDialog(item)
            }
        }

        binding.cardDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete ${item.name}?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.onDeleteFoodItem(item)
                    dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FoodDetailBottomSheet"
        private const val ARG_ITEM_ID = "arg_item_id"
        private const val ARG_ITEM_NAME = "arg_item_name"
        private const val ARG_ITEM_CATEGORY = "arg_item_category"
        private const val ARG_ITEM_EXPIRY_DATE = "arg_item_expiry_date"
        private const val ARG_ITEM_QUANTITY = "arg_item_quantity"
        private const val ARG_ITEM_LOCATION = "arg_item_location"
        private const val ARG_ITEM_NOTES = "arg_item_notes"
        private const val ARG_ITEM_RISK = "arg_item_risk"
        private const val ARG_ITEM_IMAGE_PATH = "arg_item_image_path"

        fun newInstance(item: FoodItem): FoodDetailBottomSheet {
            return FoodDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, item.id)
                    putString(ARG_ITEM_NAME, item.name)
                    putString(ARG_ITEM_CATEGORY, item.category.name)
                    putString(ARG_ITEM_EXPIRY_DATE, item.expiryDate.toString())
                    putInt(ARG_ITEM_QUANTITY, item.quantity)
                    putString(ARG_ITEM_LOCATION, item.location.name)
                    putString(ARG_ITEM_NOTES, item.notes)
                    putString(ARG_ITEM_RISK, item.riskLevel.name)
                    putString(ARG_ITEM_IMAGE_PATH, item.imagePath)
                }
            }
        }
    }
}
