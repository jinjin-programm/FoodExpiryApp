package com.example.foodexpiryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.foodexpiryapp.data.local.database.MealPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY slot ASC")
    fun getMealPlansForDate(date: String): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, slot ASC")
    fun getMealPlansForDateRange(startDate: String, endDate: String): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE id = :id")
    suspend fun getMealPlanById(id: Long): MealPlanEntity?

    @Query("SELECT * FROM meal_plans WHERE date = :date AND slot = :slot")
    suspend fun getMealPlanByDateAndSlot(date: String, slot: String): MealPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity): Long

    @Update
    suspend fun updateMealPlan(mealPlan: MealPlanEntity)

    @Delete
    suspend fun deleteMealPlan(mealPlan: MealPlanEntity)

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealPlanById(id: Long)

    @Query("DELETE FROM meal_plans WHERE date = :date")
    suspend fun deleteMealPlansForDate(date: String)
}
