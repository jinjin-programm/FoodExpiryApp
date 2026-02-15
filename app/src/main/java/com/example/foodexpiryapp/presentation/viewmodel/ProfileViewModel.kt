package com.example.foodexpiryapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodexpiryapp.domain.model.DietaryPreference
import com.example.foodexpiryapp.domain.model.UserProfile
import com.example.foodexpiryapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userProfile: UserProfile = UserProfile(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

sealed class ProfileEvent {
    data class ShowMessage(val message: String) : ProfileEvent()
    object SaveSuccess : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUserProfile()
                .take(1) // Just get the initial values
                .collect { profile ->
                    _uiState.update { 
                        it.copy(userProfile = profile, isLoading = false) 
                    }
                }
        }
    }

    fun updateName(name: String) {
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(name = name))
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(email = email))
        }
    }

    fun updateHouseholdSize(size: Int) {
        val coercedSize = size.coerceIn(1, 10)
        _uiState.update { state ->
            state.copy(userProfile = state.userProfile.copy(householdSize = coercedSize))
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
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                userRepository.saveUserProfile(_uiState.value.userProfile)
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(ProfileEvent.SaveSuccess)
                _events.emit(ProfileEvent.ShowMessage("Profile saved successfully!"))
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(ProfileEvent.ShowMessage("Failed to save profile: ${e.message}"))
            }
        }
    }
}
