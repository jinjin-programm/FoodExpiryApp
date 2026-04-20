package com.example.foodexpiryapp.data.remote.lmstudio

import com.example.foodexpiryapp.util.AppLog
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
class LmStudioServerConfig @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "LmStudioServerConfig"
        private const val DEFAULT_BASE_URL = "http://localhost:1234"
        private const val DEFAULT_MODEL_NAME = "qwen3.5-9b"

        private val KEY_BASE_URL = stringPreferencesKey("lmstudio_base_url")
        private val KEY_MODEL_NAME = stringPreferencesKey("lmstudio_model_name")
    }

    val baseUrl: Flow<String> = dataStore.data.map { it[KEY_BASE_URL] ?: DEFAULT_BASE_URL }
    val modelName: Flow<String> = dataStore.data.map { it[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME }

    val config: Flow<LmStudioConfig> = dataStore.data.map {
        LmStudioConfig(
            baseUrl = it[KEY_BASE_URL] ?: DEFAULT_BASE_URL,
            modelName = it[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME
        )
    }

    suspend fun getBaseUrl(): String = baseUrl.first()
    suspend fun getModelName(): String = modelName.first()
    suspend fun getConfig(): LmStudioConfig = config.first()

    suspend fun setBaseUrl(url: String) {
        AppLog.d(TAG, "Setting base URL: $url")
        dataStore.edit { it[KEY_BASE_URL] = url.trimEnd('/') }
    }

    suspend fun setModelName(name: String) {
        AppLog.d(TAG, "Setting model name: $name")
        dataStore.edit { it[KEY_MODEL_NAME] = name }
    }

    suspend fun updateConfig(newConfig: LmStudioConfig) {
        setBaseUrl(newConfig.baseUrl)
        setModelName(newConfig.modelName)
    }

    data class LmStudioConfig(
        val baseUrl: String = DEFAULT_BASE_URL,
        val modelName: String = DEFAULT_MODEL_NAME
    )
}
