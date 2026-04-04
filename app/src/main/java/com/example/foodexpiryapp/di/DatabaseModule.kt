package com.example.foodexpiryapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import com.example.foodexpiryapp.data.local.dao.FoodItemDao
import com.example.foodexpiryapp.data.local.dao.MealPlanDao
import com.example.foodexpiryapp.data.local.dao.ShoppingItemDao
import com.example.foodexpiryapp.data.local.dao.CookedRecipeDao
import com.example.foodexpiryapp.data.local.dao.LocalRecipeDao
import com.example.foodexpiryapp.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE food_items ADD COLUMN notifyEnabled INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE food_items ADD COLUMN notifyDaysBefore INTEGER")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "food_expiry_db"
        ).addMigrations(
            MIGRATION_1_2, 
            AppDatabase.MIGRATION_2_3, 
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7
        ).build()
    }

    @Provides
    @Singleton
    fun provideFoodItemDao(database: AppDatabase): FoodItemDao {
        return database.foodItemDao()
    }

    @Provides
    @Singleton
    fun provideMealPlanDao(database: AppDatabase): MealPlanDao {
        return database.mealPlanDao()
    }

    @Provides
    @Singleton
    fun provideShoppingItemDao(database: AppDatabase): ShoppingItemDao {
        return database.shoppingItemDao()
    }

    @Provides
    @Singleton
    fun provideCookedRecipeDao(database: AppDatabase): CookedRecipeDao {
        return database.cookedRecipeDao()
    }

    @Provides
    @Singleton
    fun provideLocalRecipeDao(database: AppDatabase): LocalRecipeDao {
        return database.localRecipeDao()
    }
}
