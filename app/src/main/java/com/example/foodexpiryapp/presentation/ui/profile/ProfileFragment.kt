package com.example.foodexpiryapp.presentation.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.foodexpiryapp.databinding.FragmentProfileBinding
import com.example.foodexpiryapp.domain.model.DietaryPreference
import com.example.foodexpiryapp.presentation.viewmodel.ProfileEvent
import com.example.foodexpiryapp.presentation.viewmodel.ProfileViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChips()
        setupListeners()
        observeState()
        observeEvents()
    }

    private fun setupChips() {
        binding.chipGroupDiet.removeAllViews()
        DietaryPreference.values().forEach { preference ->
            val chip = Chip(requireContext()).apply {
                text = preference.displayName
                isCheckable = true
                id = View.generateViewId()
                tag = preference
                setOnCheckedChangeListener { _, isChecked ->
                    // Guard: only toggle if the new state differs from the model
                    val isAlreadyChecked = viewModel.uiState.value.userProfile.dietaryPreferences.contains(preference)
                    if (isChecked != isAlreadyChecked) {
                        viewModel.toggleDietaryPreference(preference)
                    }
                }
            }
            binding.chipGroupDiet.addView(chip)
        }
    }

    private fun setupListeners() {
        binding.editName.doAfterTextChanged { text ->
            val newName = text?.toString() ?: ""
            if (newName != viewModel.uiState.value.userProfile.name) {
                viewModel.updateName(newName)
            }
        }
        binding.editEmail.doAfterTextChanged { text ->
            val newEmail = text?.toString() ?: ""
            if (newEmail != viewModel.uiState.value.userProfile.email) {
                viewModel.updateEmail(newEmail)
            }
        }
        binding.sliderHouseholdSize.addOnChangeListener { _, value, _ ->
            val size = value.toInt()
            if (size != viewModel.uiState.value.userProfile.householdSize) {
                binding.textHouseholdDesc.text = "Household size: $size person${if (size > 1) "s" else ""}"
                viewModel.updateHouseholdSize(size)
            }
        }
        
        // Notification settings listeners
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNotificationsEnabled(isChecked)
        }
        
        binding.sliderDaysBefore.addOnChangeListener { _, value, _ ->
            val days = value.toInt()
            binding.textDaysBeforeLabel.text = "Notify days before expiry: $days day${if (days > 1) "s" else ""}"
            viewModel.updateDefaultDaysBefore(days)
        }
        
        binding.btnSetTime.setOnClickListener {
            showTimePicker()
        }
        
        binding.btnSave.setOnClickListener {
            viewModel.saveProfile()
        }
    }
    
    private fun showTimePicker() {
        val currentSettings = viewModel.uiState.value.notificationSettings
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentSettings.notificationHour)
            .setMinute(currentSettings.notificationMinute)
            .setTitleText("Select notification time")
            .build()
        
        picker.addOnPositiveButtonClickListener {
            viewModel.updateNotificationTime(picker.hour, picker.minute)
        }
        
        picker.show(parentFragmentManager, "time_picker")
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Guarded updates to prevent infinite loops and cursor jumps
                    if (binding.editName.text?.toString() != state.userProfile.name) {
                        binding.editName.setText(state.userProfile.name)
                    }
                    if (binding.editEmail.text?.toString() != state.userProfile.email) {
                        binding.editEmail.setText(state.userProfile.email)
                    }
                    
                    val householdSize = state.userProfile.householdSize.toFloat()
                    if (binding.sliderHouseholdSize.value != householdSize) {
                        binding.sliderHouseholdSize.value = householdSize
                    }
                    binding.textHouseholdDesc.text = "Household size: ${state.userProfile.householdSize} person${if (state.userProfile.householdSize > 1) "s" else ""}"

                    // Update chips
                    for (i in 0 until binding.chipGroupDiet.childCount) {
                        val chip = binding.chipGroupDiet.getChildAt(i) as Chip
                        val preference = chip.tag as DietaryPreference
                        val shouldBeChecked = state.userProfile.dietaryPreferences.contains(preference)
                        if (chip.isChecked != shouldBeChecked) {
                            chip.isChecked = shouldBeChecked
                        }
                    }
                    
                    // Update notification settings UI
                    val settings = state.notificationSettings
                    if (binding.switchNotifications.isChecked != settings.notificationsEnabled) {
                        binding.switchNotifications.isChecked = settings.notificationsEnabled
                    }
                    
                    val daysBefore = settings.defaultDaysBefore.toFloat()
                    if (binding.sliderDaysBefore.value != daysBefore) {
                        binding.sliderDaysBefore.value = daysBefore
                    }
                    binding.textDaysBeforeLabel.text = "Notify days before expiry: ${settings.defaultDaysBefore} day${if (settings.defaultDaysBefore > 1) "s" else ""}"
                    
                    val timeText = formatTime(settings.notificationHour, settings.notificationMinute)
                    binding.textNotificationTimeLabel.text = "Daily reminder time: $timeText"

                    // Progress
                    binding.progressIndicator.visibility = if (state.isSaving || state.isLoading) View.VISIBLE else View.GONE
                    binding.btnSave.isEnabled = !state.isSaving
                }
            }
        }
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ProfileEvent.ShowMessage -> {
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        }
                        is ProfileEvent.SaveSuccess -> {
                            binding.editName.clearFocus()
                            binding.editEmail.clearFocus()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
