package com.example.foodexpiryapp.di

import android.content.Context
import com.example.foodexpiryapp.data.local.ModelStorageManager
import com.example.foodexpiryapp.data.repository.LlmInferenceRepositoryImpl
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import com.example.foodexpiryapp.inference.mnn.MnnLlmConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for MNN inference dependencies.
 * Per D-03: Binds domain interface to data implementation.
 *
 * Provides [MnnLlmConfig] via factory method since it is a data class
 * without an @Inject-annotated constructor.
 *
 * [MnnLlmEngine], [ModelStorageManager], and [ModelLifecycleManager]
 * are @Singleton @Inject classes, so they're automatically provided by Hilt.
 * This module binds the repository interface and provides the config.
 */
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
        fun provideMnnLlmConfig(
            @ApplicationContext context: Context,
            storageManager: ModelStorageManager
        ): MnnLlmConfig {
            val modelDir = storageManager.getModelDirectory().absolutePath
            return MnnLlmConfig.createOptimal().copy(modelDirPath = modelDir)
        }
    }
}
