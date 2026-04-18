package com.example.foodexpiryapp.presentation.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.foodexpiryapp.R
import com.example.foodexpiryapp.databinding.FragmentProfileBinding
import com.example.foodexpiryapp.domain.model.DietaryPreference
import com.example.foodexpiryapp.presentation.viewmodel.ProfileEvent
import com.example.foodexpiryapp.presentation.viewmodel.ProfileViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodexpiryapp.worker.ExpiryNotificationWorker
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.updateProfilePhoto(uri.toString())
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerTestNotification()
        } else {
            Toast.makeText(requireContext(), "Notification permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 9001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupGoogleSignIn()
        setupChips()
        setupListeners()
        observeState()
        observeEvents()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            viewModel.updateGoogleSignInState(
                isSignedIn = true,
                displayName = account.displayName,
                email = account.email,
                photoUrl = account.photoUrl?.toString()
            )
        }
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
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGoogleSignOut.setOnClickListener {
            signOutFromGoogle()
        }
        
        binding.btnEditProfile.setOnClickListener {
            pickImage.launch("image/*")
        }
        
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
                binding.textHouseholdDesc.text = "$size person${if (size > 1) "s" else ""}"
                viewModel.updateHouseholdSize(size)
            }
        }
        
        // Notification settings listeners
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNotificationsEnabled(isChecked)
        }
        
        binding.sliderDaysBefore.addOnChangeListener { _, value, _ ->
            val days = value.toInt()
            binding.textDaysBeforeLabel.text = "$days Days"
            viewModel.updateDefaultDaysBefore(days)
        }
        
        binding.btnSetTime.setOnClickListener {
            showTimePicker()
        }
        
        binding.btnSave.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Changes")
                .setMessage("Are you sure you want to save your profile changes?")
                .setPositiveButton("Confirm") { dialog, _ ->
                    viewModel.saveProfile()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        binding.btnTestNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    triggerTestNotification()
                } else {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                triggerTestNotification()
            }
        }

        binding.btnShelfLifeManagement.setOnClickListener {
            findNavController().navigate(R.id.shelfLifeManagement)
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

    private fun triggerTestNotification() {
        val request = OneTimeWorkRequestBuilder<ExpiryNotificationWorker>().build()
        WorkManager.getInstance(requireContext()).enqueue(request)
        Toast.makeText(requireContext(), "Test notification sent!", Toast.LENGTH_SHORT).show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update Google Sign-In UI
                    if (state.isGoogleSignIn) {
                        binding.btnGoogleSignIn.visibility = View.GONE
                        binding.textGoogleSignInHint.visibility = View.GONE
                        binding.btnGoogleSignOut.visibility = View.VISIBLE
                        
                        // Disable manual email editing when signed in with Google
                        binding.editEmail.isEnabled = false
                    } else {
                        binding.btnGoogleSignIn.visibility = View.VISIBLE
                        binding.textGoogleSignInHint.visibility = View.VISIBLE
                        binding.btnGoogleSignOut.visibility = View.GONE
                        binding.editEmail.isEnabled = true
                    }
                    
                    // Guarded updates to prevent infinite loops and cursor jumps
                    if (binding.editName.text?.toString() != state.userProfile.name) {
                        binding.editName.setText(state.userProfile.name)
                    }
                    if (binding.editEmail.text?.toString() != state.userProfile.email) {
                        binding.editEmail.setText(state.userProfile.email)
                    }
                    
                    val householdSize = state.userProfile.householdSize.toFloat().coerceIn(1f, 10f)
                    if (binding.sliderHouseholdSize.value != householdSize) {
                        binding.sliderHouseholdSize.value = householdSize
                    }
                    binding.textHouseholdDesc.text = "${state.userProfile.householdSize} person${if (state.userProfile.householdSize > 1) "s" else ""}"

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
                    
                    // Update profile photo
                    val photoUri = state.userProfile.profilePhotoUri
                    if (!photoUri.isNullOrEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(photoUri)
                            .centerCrop()
                            .into(binding.imgProfile)
                    } else {
                        binding.imgProfile.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                    
                    if (binding.switchNotifications.isChecked != settings.notificationsEnabled) {
                        binding.switchNotifications.isChecked = settings.notificationsEnabled
                    }
                    
                    val daysBefore = settings.defaultDaysBefore.toFloat().coerceIn(1f, 14f)
                    if (binding.sliderDaysBefore.value != daysBefore) {
                        binding.sliderDaysBefore.value = daysBefore
                    }
                    binding.textDaysBeforeLabel.text = "${settings.defaultDaysBefore} Days"
                    
                    val timeText = formatTime(settings.notificationHour, settings.notificationMinute)
                    binding.textNotificationTimeLabel.text = timeText

                    // Progress
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

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    private fun signOutFromGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            viewModel.signOutGoogle()
            Snackbar.make(binding.root, "Signed out successfully", Snackbar.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.updateGoogleSignInState(
                    isSignedIn = true,
                    displayName = account.displayName,
                    email = account.email,
                    photoUrl = account.photoUrl?.toString()
                )
                Snackbar.make(binding.root, "Signed in as ${account.email}", Snackbar.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Snackbar.make(binding.root, "Google Sign-In failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
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
