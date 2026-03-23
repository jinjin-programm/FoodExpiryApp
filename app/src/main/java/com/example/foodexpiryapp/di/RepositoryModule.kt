package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.data.repository.FoodRepositoryImpl
import com.example.foodexpiryapp.data.repository.MealPlanRepositoryImpl
import com.example.foodexpiryapp.data.repository.NotificationSettingsRepositoryImpl
import com.example.foodexpiryapp.data.repository.RecipeRepositoryImpl
import com.example.foodexpiryapp.data.repository.UserRepositoryImpl
import com.example.foodexpiryapp.domain.repository.FoodRepository
import com.example.foodexpiryapp.domain.repository.MealPlanRepository
import com.example.foodexpiryapp.domain.repository.NotificationSettingsRepository
import com.example.foodexpiryapp.domain.repository.RecipeRepository
import com.example.foodexpiryapp.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindNotificationSettingsRepository(impl: NotificationSettingsRepositoryImpl): NotificationSettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(impl: MealPlanRepositoryImpl): MealPlanRepository
}
