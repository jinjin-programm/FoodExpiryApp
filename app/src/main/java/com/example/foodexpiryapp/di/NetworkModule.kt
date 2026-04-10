package com.example.foodexpiryapp.di

import com.example.foodexpiryapp.BuildConfig
import com.example.foodexpiryapp.data.remote.HfTokenProvider
import com.example.foodexpiryapp.data.remote.OpenFoodFactsApi
import com.example.foodexpiryapp.data.remote.TheMealDbApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }

    @Provides
    @Singleton
    fun provideHfTokenProvider(): HfTokenProvider {
        return object : HfTokenProvider {
            override fun getToken(): String = BuildConfig.HF_TOKEN
        }
    }
    
    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(gson: Gson): OpenFoodFactsApi {
        return Retrofit.Builder()
            .baseUrl(OpenFoodFactsApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTheMealDbApi(gson: Gson): TheMealDbApi {
        return Retrofit.Builder()
            .baseUrl(TheMealDbApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TheMealDbApi::class.java)
    }
}