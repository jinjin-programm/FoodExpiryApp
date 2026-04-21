package com.example.foodexpiryapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.foodexpiryapp.domain.model.NotificationSettings
import com.example.foodexpiryapp.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : NotificationSettingsRepository {

    companion object {
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val DEFAULT_DAYS_BEFORE = intPreferencesKey("default_days_before")
        private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    }

    override fun getNotificationSettings(): Flow<NotificationSettings> {
        return dataStore.data.map { preferences ->
            NotificationSettings(
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
                defaultDaysBefore = preferences[DEFAULT_DAYS_BEFORE] ?: 3,
                notificationHour = preferences[NOTIFICATION_HOUR] ?: 9,
                notificationMinute = preferences[NOTIFICATION_MINUTE] ?: 0
            )
        }
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override suspend fun updateDefaultDaysBefore(days: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_DAYS_BEFORE] = days.coerceIn(1, 30)
        }
    }

    override suspend fun updateNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR] = hour.coerceIn(0, 23)
            preferences[NOTIFICATION_MINUTE] = minute.coerceIn(0, 59)
        }
    }
}
