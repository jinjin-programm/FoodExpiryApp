package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.DietaryPreference
import com.example.foodexpiryapp.domain.model.NotificationSettings
import com.example.foodexpiryapp.domain.model.UserProfile
import com.example.foodexpiryapp.domain.repository.NotificationSettingsRepository
import com.example.foodexpiryapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val hasUnsavedChanges: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val selectedProfilePhotoUri: String? = null,
    val isFirstTimeSetup: Boolean = false
)

sealed class ProfileEvent {
    data class ShowMessage(val message: String) : ProfileEvent()
    object SaveSuccess : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

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
                    _uiState.update { it.copy(notificationSettings = settings) }
                }
        }
    }

    fun updateName(name: String) {
        _uiState.update { state ->
            state.copy(
                userProfile = state.userProfile.copy(name = name),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateValidationError(fieldName: String, error: String?) {
        _uiState.update { state ->
            val errors = state.validationErrors.toMutableMap()
            if (error == null) {
                errors.remove(fieldName)
            } else {
                errors[fieldName] = error
            }
            state.copy(validationErrors = errors.toMap())
        }
    }

    fun updateProfilePhoto(uri: String) {
        _uiState.update { state ->
            state.copy(
                selectedProfilePhotoUri = uri,
                userProfile = state.userProfile.copy(profilePhotoUri = uri),
                hasUnsavedChanges = true
            )
        }
    }

    fun discardChanges() {
        loadUserProfile()
        loadNotificationSettings()
        _uiState.update { state ->
            state.copy(
                hasUnsavedChanges = false,
                selectedProfilePhotoUri = null,
                validationErrors = emptyMap()
            )
        }
    }

    fun setFirstTimeSetup(value: Boolean) {
        _uiState.update { it.copy(isFirstTimeSetup = value) }
    }

    fun updateHouseholdSize(size: Int) {
        val coercedSize = size.coerceIn(1, 10)
        _uiState.update { state ->
            state.copy(
                userProfile = state.userProfile.copy(householdSize = coercedSize),
                hasUnsavedChanges = true
            )
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
            state.copy(
                userProfile = state.userProfile.copy(dietaryPreferences = currentPrefs),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                notificationSettings = state.notificationSettings.copy(notificationsEnabled = enabled),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateDefaultDaysBefore(days: Int) {
        _uiState.update { state ->
            state.copy(
                notificationSettings = state.notificationSettings.copy(defaultDaysBefore = days),
                hasUnsavedChanges = true
            )
        }
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        _uiState.update { state ->
            state.copy(
                notificationSettings = state.notificationSettings.copy(
                    notificationHour = hour,
                    notificationMinute = minute
                ),
                hasUnsavedChanges = true
            )
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
                
                _uiState.update { it.copy(isSaving = false) }
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
