package com.example.foodexpiryapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.foodexpiryapp.domain.model.DietaryPreference
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
            
            val dietaryPreferences = dietStrings.mapNotNull { dietName ->
                try {
                    DietaryPreference.valueOf(dietName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()

            UserProfile(name, email, householdSize, dietaryPreferences)
        }

    override suspend fun saveUserProfile(profile: UserProfile) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = profile.name
            preferences[PreferencesKeys.USER_EMAIL] = profile.email
            preferences[PreferencesKeys.HOUSEHOLD_SIZE] = profile.householdSize
            preferences[PreferencesKeys.DIETARY_PREFERENCES] = profile.dietaryPreferences.map { it.name }.toSet()
        }
    }
}
