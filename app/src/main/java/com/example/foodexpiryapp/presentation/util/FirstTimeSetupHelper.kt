package com.example.foodexpiryapp.presentation.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val APP_SETTINGS_NAME = "app_settings"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = APP_SETTINGS_NAME)

object FirstTimeSetupHelper {
    private val IS_FIRST_TIME_KEY = booleanPreferencesKey("is_first_time_launch")

    /**
     * Check if this is the first time the app is being launched.
     */
    fun isFirstTimeLaunch(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_FIRST_TIME_KEY] ?: true
        }
    }

    /**
     * Mark that the first-time setup has been completed.
     */
    suspend fun markFirstTimeSetupComplete(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME_KEY] = false
        }
    }
}
