package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.data.repository.LlmInferenceRepositoryImpl
import com.example.foodexpiryapp.domain.repository.LlmInferenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for MNN inference dependencies.
 * Per D-03: Binds domain interface to data implementation.
 *
 * Note: [com.example.foodexpiryapp.inference.mnn.MnnLlmEngine] and
 * [com.example.foodexpiryapp.data.remote.ModelDownloadManager] are @Singleton @Inject
 * classes, so they're automatically provided by Hilt. This module only needs
 * to bind the repository interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {

    @Binds
    @Singleton
    abstract fun bindLlmInferenceRepository(
        impl: LlmInferenceRepositoryImpl
    ): LlmInferenceRepository
}
