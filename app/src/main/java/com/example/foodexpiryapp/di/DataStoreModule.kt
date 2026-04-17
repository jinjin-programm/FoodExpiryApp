package com.example.foodexpiryapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioServerConfig
import com.example.foodexpiryapp.data.remote.ollama.OllamaServerConfig
import com.example.foodexpiryapp.data.remote.ProviderConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    private const val USER_PREFERENCES_NAME = "user_preferences"

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(USER_PREFERENCES_NAME) }
        )
    }

    @Provides
    @Singleton
    fun provideOllamaServerConfig(dataStore: DataStore<Preferences>): OllamaServerConfig {
        return OllamaServerConfig(dataStore)
    }

    @Provides
    @Singleton
    fun provideLmStudioServerConfig(dataStore: DataStore<Preferences>): LmStudioServerConfig {
        return LmStudioServerConfig(dataStore)
    }

    @Provides
    @Singleton
    fun provideProviderConfig(dataStore: DataStore<Preferences>): ProviderConfig {
        return ProviderConfig(dataStore)
    }
}
