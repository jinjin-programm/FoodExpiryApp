package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.data.repository.YoloDetectionRepositoryImpl
import com.example.foodexpiryapp.domain.repository.YoloDetectionRepository
import com.example.foodexpiryapp.inference.yolo.MnnYoloConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for detection pipeline dependencies.
 *
 * Per D-03: Binds domain interface to data implementation.
 *
 * Provides:
 * - [YoloDetectionRepository] binding to [YoloDetectionRepositoryImpl]
 * - [MnnYoloConfig] with default configuration
 *
 * [DetectionPipeline], [MnnYoloEngine], and [MnnLlmEngine] are
 * @Singleton @Inject classes, so they're automatically provided by Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DetectionModule {

    @Binds
    @Singleton
    abstract fun bindYoloDetectionRepository(
        impl: YoloDetectionRepositoryImpl
    ): YoloDetectionRepository

    companion object {
        @Provides
        @Singleton
        fun provideMnnYoloConfig(): MnnYoloConfig = MnnYoloConfig()
    }
}
