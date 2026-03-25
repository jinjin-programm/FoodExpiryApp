package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.MealPlanDao
import com.example.foodexpiryapp.data.mapper.MealPlanMapper
import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot
import com.example.foodexpiryapp.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanRepositoryImpl @Inject constructor(
    private val dao: MealPlanDao
) : MealPlanRepository {

    override fun getMealPlansForDate(date: LocalDate): Flow<List<MealPlan>> {
        return dao.getMealPlansForDate(date.toString()).map { entities ->
            entities.map { MealPlanMapper.entityToDomain(it) }
        }
    }

    override fun getMealPlansForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MealPlan>> {
        return dao.getMealPlansForDateRange(startDate.toString(), endDate.toString()).map { entities ->
            entities.map { MealPlanMapper.entityToDomain(it) }
        }
    }

    override suspend fun getMealPlanById(id: Long): MealPlan? {
        return dao.getMealPlanById(id)?.let { MealPlanMapper.entityToDomain(it) }
    }

    override suspend fun getMealPlanByDateAndSlot(date: LocalDate, slot: MealSlot): MealPlan? {
        return dao.getMealPlanByDateAndSlot(date.toString(), slot.name)?.let {
            MealPlanMapper.entityToDomain(it)
        }
    }

    override suspend fun insertMealPlan(mealPlan: MealPlan): Long {
        return dao.insertMealPlan(MealPlanMapper.domainToEntity(mealPlan))
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan) {
        dao.updateMealPlan(MealPlanMapper.domainToEntity(mealPlan))
    }

    override suspend fun deleteMealPlan(mealPlan: MealPlan) {
        dao.deleteMealPlan(MealPlanMapper.domainToEntity(mealPlan))
    }

    override suspend fun deleteMealPlanById(id: Long) {
        dao.deleteMealPlanById(id)
    }

    override suspend fun deleteMealPlansForDate(date: LocalDate) {
        dao.deleteMealPlansForDate(date.toString())
    }
}
