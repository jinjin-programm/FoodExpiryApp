package com.example.foodexpiryapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FoodItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodItemDao(): com.example.foodexpiryapp.data.local.dao.FoodItemDao
}
