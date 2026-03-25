package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MealPlanRepository {

    fun getMealPlansForDate(date: LocalDate): Flow<List<MealPlan>>

    fun getMealPlansForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MealPlan>>

    suspend fun getMealPlanById(id: Long): MealPlan?

    suspend fun getMealPlanByDateAndSlot(date: LocalDate, slot: MealSlot): MealPlan?

    suspend fun insertMealPlan(mealPlan: MealPlan): Long

    suspend fun updateMealPlan(mealPlan: MealPlan)

    suspend fun deleteMealPlan(mealPlan: MealPlan)

    suspend fun deleteMealPlanById(id: Long)

    suspend fun deleteMealPlansForDate(date: LocalDate)
}
