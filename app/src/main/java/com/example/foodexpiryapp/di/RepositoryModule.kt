package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.data.repository.FoodRepositoryImpl
import com.example.foodexpiryapp.data.repository.UserRepositoryImpl
import com.example.foodexpiryapp.domain.repository.FoodRepository
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
}
