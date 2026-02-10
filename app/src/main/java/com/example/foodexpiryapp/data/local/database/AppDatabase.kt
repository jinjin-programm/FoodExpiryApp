package com.example.foodexpiryapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the FoodExpiryApp.
 * Increment version when schema changes and add a migration.
 */
@Database(
    entities = [FoodItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): com.example.foodexpiryapp.data.local.dao.FoodItemDao
}
