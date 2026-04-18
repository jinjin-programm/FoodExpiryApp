package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioApiClient
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioServerConfig
import com.example.foodexpiryapp.data.remote.lmstudio.LmStudioVisionClient
import com.example.foodexpiryapp.data.remote.ollama.OllamaApiClient
import com.example.foodexpiryapp.data.remote.ollama.OllamaServerConfig
import com.example.foodexpiryapp.data.remote.ollama.OllamaVisionClient
import com.example.foodexpiryapp.data.repository.LlmInferenceRepositoryImpl
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import com.example.foodexpiryapp.inference.tflite.OnnxYoloEngine
import com.example.foodexpiryapp.inference.tflite.TFLiteYoloEngine
import com.example.foodexpiryapp.inference.tflite.YoloDetector
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindLlmInferenceRepository(
        impl: LlmInferenceRepositoryImpl
    ): LlmInferenceRepository

    companion object {
        @Provides
        @Singleton
        fun provideOllamaApiClient(
            serverConfig: OllamaServerConfig,
            gson: Gson
        ): OllamaApiClient {
            return OllamaApiClient(serverConfig, gson)
        }

        @Provides
        @Singleton
        fun provideOllamaVisionClient(
            apiClient: OllamaApiClient,
            serverConfig: OllamaServerConfig
        ): OllamaVisionClient {
            return OllamaVisionClient(apiClient, serverConfig)
        }

        @Provides
        @Singleton
        fun provideLmStudioApiClient(
            serverConfig: LmStudioServerConfig,
            gson: Gson
        ): LmStudioApiClient {
            return LmStudioApiClient(serverConfig, gson)
        }

        @Provides
        @Singleton
        fun provideLmStudioVisionClient(
            apiClient: LmStudioApiClient,
            serverConfig: LmStudioServerConfig
        ): LmStudioVisionClient {
            return LmStudioVisionClient(apiClient, serverConfig)
        }

        @Provides
        @Singleton
        fun provideYoloDetector(
            engine: OnnxYoloEngine
        ): YoloDetector {
            return engine
        }
    }
}
