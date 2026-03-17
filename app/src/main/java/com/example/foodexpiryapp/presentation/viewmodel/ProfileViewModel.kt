package com.example.foodexpiryapp.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.DietaryPreference
import com.example.foodexpiryapp.domain.model.NotificationSettings
import com.example.foodexpiryapp.domain.model.UserProfile
import com.example.foodexpiryapp.domain.repository.NotificationSettingsRepository
import com.example.foodexpiryapp.domain.repository.UserRepository
import com.example.foodexpiryapp.util.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isGoogleSignIn: Boolean = false,
    val googleUserName: String? = null,
    val googleUserEmail: String? = null,
    val googleUserPhotoUrl: String? = null,
    val validationErrors: Map<String, String?> = emptyMap(),
    val hasUnsavedChanges: Boolean = false
)

sealed class ProfileEvent {
    data class ShowMessage(val message: String) : ProfileEvent()
    object SaveSuccess : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    private var originalProfile: UserProfile? = null
    private var originalSettings: NotificationSettings? = null

    init {
        loadUserProfile()
        loadNotificationSettings()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUserProfile()
                .take(1)
                .collect { profile ->
                    originalProfile = profile
                    _uiState.update { 
                        it.copy(userProfile = profile, isLoading = false) 
                    }
                }
        }
    }

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            notificationSettingsRepository.getNotificationSettings()
                .take(1)
                .collect { settings ->
                    originalSettings = settings
                    _uiState.update { it.copy(notificationSettings = settings) }
                }
        }
    }

    private fun checkUnsavedChanges() {
        val currentProfile = _uiState.value.userProfile
        val currentSettings = _uiState.value.notificationSettings
        
        val profileChanged = originalProfile != null && currentProfile != originalProfile
        val settingsChanged = originalSettings != null && currentSettings != originalSettings
        
        _uiState.update { it.copy(hasUnsavedChanges = profileChanged || settingsChanged) }
    }

    fun updateName(name: String) {
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(name = name))
        }
        checkUnsavedChanges()
    }

    fun updateEmail(email: String) {
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(email = email))
        }
        checkUnsavedChanges()
    }

    fun updateHouseholdSize(size: Int) {
        val coercedSize = size.coerceIn(1, 10)
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(householdSize = coercedSize))
        }
        checkUnsavedChanges()
    }

    fun updateProfilePhoto(path: String) {
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(profilePhotoUri = path))
        }
        checkUnsavedChanges()
    }

    fun updateValidationError(field: String, error: String?) {
        _uiState.update { state ->
            val errors = state.validationErrors.toMutableMap()
            if (error == null) {
                errors.remove(field)
            } else {
                errors[field] = error
            }
            state.copy(validationErrors = errors)
        }
    }

    fun toggleDietaryPreference(preference: DietaryPreference) {
        _uiState.update { state ->
            val currentPrefs = state.userProfile.dietaryPreferences.toMutableSet()
            if (currentPrefs.contains(preference)) {
                currentPrefs.remove(preference)
            } else {
                currentPrefs.add(preference)
            }
            state.copy(userProfile = state.userProfile.copy(dietaryPreferences = currentPrefs))
        }
        checkUnsavedChanges()
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(notificationSettings = state.notificationSettings.copy(notificationsEnabled = enabled))
        }
        checkUnsavedChanges()
    }

    fun updateDefaultDaysBefore(days: Int) {
        _uiState.update { state ->
            state.copy(notificationSettings = state.notificationSettings.copy(defaultDaysBefore = days))
        }
        checkUnsavedChanges()
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        _uiState.update { state ->
            state.copy(notificationSettings = state.notificationSettings.copy(
                notificationHour = hour,
                notificationMinute = minute
            ))
        }
        checkUnsavedChanges()
    }

    fun discardChanges() {
        viewModelScope.launch {
            originalProfile?.let { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
            originalSettings?.let { settings ->
                _uiState.update { it.copy(notificationSettings = settings) }
            }
            _uiState.update { 
                it.copy(
                    hasUnsavedChanges = false,
                    validationErrors = emptyMap()
                ) 
            }
            _events.emit(ProfileEvent.ShowMessage("Changes discarded"))
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                userRepository.saveUserProfile(_uiState.value.userProfile)
                
                val settings = _uiState.value.notificationSettings
                notificationSettingsRepository.updateNotificationsEnabled(settings.notificationsEnabled)
                notificationSettingsRepository.updateDefaultDaysBefore(settings.defaultDaysBefore)
                notificationSettingsRepository.updateNotificationTime(settings.notificationHour, settings.notificationMinute)
                
                // Reschedule notifications with new settings
                NotificationScheduler.scheduleDailyNotification(context, settings)
                
                originalProfile = _uiState.value.userProfile
                originalSettings = _uiState.value.notificationSettings
                
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        hasUnsavedChanges = false
                    ) 
                }
                _events.emit(ProfileEvent.SaveSuccess)
                _events.emit(ProfileEvent.ShowMessage("Profile saved successfully!"))
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(ProfileEvent.ShowMessage("Failed to save profile: ${e.message}"))
            }
        }
    }

    fun updateGoogleSignInState(
        isSignedIn: Boolean,
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                isGoogleSignIn = isSignedIn,
                googleUserName = displayName,
                googleUserEmail = email,
                googleUserPhotoUrl = photoUrl,
                userProfile = if (isSignedIn && email != null) {
                    state.userProfile.copy(
                        name = displayName ?: state.userProfile.name,
                        email = email
                    )
                } else {
                    state.userProfile
                }
            )
        }
    }

    fun signOutGoogle() {
        _uiState.update { state ->
            state.copy(
                isGoogleSignIn = false,
                googleUserName = null,
                googleUserEmail = null,
                googleUserPhotoUrl = null
            )
        }
    }
}
