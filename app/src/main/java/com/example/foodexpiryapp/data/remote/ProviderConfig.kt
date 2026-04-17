package com.example.foodexpiryapp.data.remote

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderConfig @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "ProviderConfig"
        const val PROVIDER_OLLAMA = "ollama"
        const val PROVIDER_LMSTUDIO = "lmstudio"
        private val KEY_PROVIDER = stringPreferencesKey("inference_provider")
    }

    val provider: Flow<String> = dataStore.data.map { it[KEY_PROVIDER] ?: PROVIDER_OLLAMA }

    suspend fun getProvider(): String = provider.first()

    suspend fun setProvider(provider: String) {
        Log.d(TAG, "Setting provider: $provider")
        dataStore.edit { it[KEY_PROVIDER] = provider }
    }
}
