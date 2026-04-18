package com.example.foodexpiryapp.data.remote.ollama

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
class OllamaServerConfig @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "OllamaServerConfig"
        private const val DEFAULT_BASE_URL = "https://roy-struggle-menus-records.trycloudflare.com"
        private const val DEFAULT_MODEL_NAME = "qwen3.5:9b"
        
        private val KEY_BASE_URL = stringPreferencesKey("ollama_base_url")
        private val KEY_MODEL_NAME = stringPreferencesKey("ollama_model_name")
        private val KEY_API_TOKEN = stringPreferencesKey("ollama_api_token")
    }
    
    val baseUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }
    
    val modelName: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME
    }
    
    val apiToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_API_TOKEN]
    }
    
    val config: Flow<OllamaConfig> = dataStore.data.map { preferences ->
        OllamaConfig(
            baseUrl = preferences[KEY_BASE_URL] ?: DEFAULT_BASE_URL,
            modelName = preferences[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME,
            apiToken = preferences[KEY_API_TOKEN]
        )
    }
    
    suspend fun getBaseUrl(): String = baseUrl.first()
    
    suspend fun getModelName(): String = modelName.first()
    
    suspend fun getApiToken(): String? = apiToken.first()
    
    suspend fun getConfig(): OllamaConfig = config.first()
    
    suspend fun setBaseUrl(url: String) {
        Log.d(TAG, "Setting base URL: $url")
        dataStore.edit { preferences ->
            preferences[KEY_BASE_URL] = url.trimEnd('/')
        }
    }
    
    suspend fun setModelName(name: String) {
        Log.d(TAG, "Setting model name: $name")
        dataStore.edit { preferences ->
            preferences[KEY_MODEL_NAME] = name
        }
    }
    
    suspend fun setApiToken(token: String?) {
        Log.d(TAG, "Setting API token: ${if (token != null) "***" else "null"}")
        dataStore.edit { preferences ->
            if (token.isNullOrBlank()) {
                preferences.remove(KEY_API_TOKEN)
            } else {
                preferences[KEY_API_TOKEN] = token
            }
        }
    }
    
    suspend fun updateConfig(newConfig: OllamaConfig) {
        setBaseUrl(newConfig.baseUrl)
        setModelName(newConfig.modelName)
        setApiToken(newConfig.apiToken)
    }
    
    data class OllamaConfig(
        val baseUrl: String = DEFAULT_BASE_URL,
        val modelName: String = DEFAULT_MODEL_NAME,
        val apiToken: String? = null
    )
}