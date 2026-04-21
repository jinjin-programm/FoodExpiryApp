package com.example.foodexpiryapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.foodexpiryapp.domain.model.DietaryPreference
import com.example.foodexpiryapp.domain.model.FoodAllergen
import com.example.foodexpiryapp.domain.model.UserAllergens
import com.example.foodexpiryapp.domain.model.UserProfile
import com.example.foodexpiryapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserRepository {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val HOUSEHOLD_SIZE = intPreferencesKey("household_size")
        val DIETARY_PREFERENCES = stringSetPreferencesKey("dietary_preferences")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri")
        val PRESET_ALLERGENS = stringSetPreferencesKey("preset_allergens")
        val CUSTOM_ALLERGENS = stringSetPreferencesKey("custom_allergens")
    }

    override fun getUserProfile(): Flow<UserProfile> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[PreferencesKeys.USER_NAME] ?: ""
            val email = preferences[PreferencesKeys.USER_EMAIL] ?: ""
            val householdSize = (preferences[PreferencesKeys.HOUSEHOLD_SIZE] ?: 1).coerceIn(1, 10)
            val dietStrings = preferences[PreferencesKeys.DIETARY_PREFERENCES] ?: emptySet()
            val profilePhotoUri = preferences[PreferencesKeys.PROFILE_PHOTO_URI]
            val allergenStrings = preferences[PreferencesKeys.PRESET_ALLERGENS] ?: emptySet()
            val customAllergenStrings = preferences[PreferencesKeys.CUSTOM_ALLERGENS] ?: emptySet()
            
            val dietaryPreferences = dietStrings.mapNotNull { dietName ->
                try {
                    DietaryPreference.valueOf(dietName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()

            val presetAllergens = allergenStrings.mapNotNull { allergenName ->
                try {
                    FoodAllergen.valueOf(allergenName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()

            val customAllergens = customAllergenStrings
            val allergens = UserAllergens(presetAllergens, customAllergens)

            UserProfile(name, email, householdSize, dietaryPreferences, profilePhotoUri, allergens)
        }

    override suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = profile.name
            preferences[PreferencesKeys.USER_EMAIL] = profile.email
            preferences[PreferencesKeys.HOUSEHOLD_SIZE] = profile.householdSize
            preferences[PreferencesKeys.DIETARY_PREFERENCES] = profile.dietaryPreferences.map { it.name }.toSet()
            profile.profilePhotoUri?.let { 
                preferences[PreferencesKeys.PROFILE_PHOTO_URI] = it 
            } ?: preferences.remove(PreferencesKeys.PROFILE_PHOTO_URI)
            preferences[PreferencesKeys.PRESET_ALLERGENS] = profile.allergens.presetAllergens.map { it.name }.toSet()
            preferences[PreferencesKeys.CUSTOM_ALLERGENS] = profile.allergens.customAllergens
        }
    }
}
