package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.data.analytics.FoodAnalyticsService
import com.example.foodexpiryapp.data.local.dao.AnalyticsEventDao
import com.example.foodexpiryapp.data.local.database.AppDatabase
import com.example.foodexpiryapp.data.repository.AnalyticsRepositoryImpl
import com.example.foodexpiryapp.domain.repository.AnalyticsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideAnalyticsEventDao(database: AppDatabase): AnalyticsEventDao {
        return database.analyticsEventDao()
    }
    
    @Provides
    @Singleton
    fun provideAnalyticsRepository(
        analyticsEventDao: AnalyticsEventDao,
        analyticsService: FoodAnalyticsService
    ): AnalyticsRepository {
        return AnalyticsRepositoryImpl(analyticsEventDao, analyticsService)
    }
    
    @Provides
    @Singleton
    fun provideFoodAnalyticsService(): FoodAnalyticsService {
        return FoodAnalyticsService()
    }
}
