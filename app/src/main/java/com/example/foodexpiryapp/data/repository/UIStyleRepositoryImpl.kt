package com.example.foodexpiryapp.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.foodexpiryapp.domain.repository.UIStyleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UIStyleRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UIStyleRepository {

    private val UI_STYLE_KEY = stringPreferencesKey("ui_style")

    override val uiStyle: Flow<String> = dataStore.data.map { prefs ->
        prefs[UI_STYLE_KEY] ?: UIStyleRepository.STYLE_CUTE
    }

    override suspend fun setUIStyle(style: String) {
        dataStore.edit { it[UI_STYLE_KEY] = style }
    }
}
