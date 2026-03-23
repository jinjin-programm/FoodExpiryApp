package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.domain.model.MealPlan
import com.example.foodexpiryapp.domain.model.MealSlot
import com.example.foodexpiryapp.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetMealPlansForDateUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<MealPlan>> {
        return repository.getMealPlansForDate(date)
    }
}

class SaveMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(mealPlan: MealPlan): Long {
        val existing = repository.getMealPlanByDateAndSlot(mealPlan.date, mealPlan.slot)
        return if (existing != null) {
            repository.updateMealPlan(mealPlan.copy(id = existing.id))
            existing.id
        } else {
            repository.insertMealPlan(mealPlan)
        }
    }
}

class DeleteMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(mealPlan: MealPlan) {
        repository.deleteMealPlan(mealPlan)
    }

    suspend fun byId(id: Long) {
        repository.deleteMealPlanById(id)
    }

    suspend fun byDateAndSlot(date: LocalDate, slot: MealSlot) {
        repository.getMealPlanByDateAndSlot(date, slot)?.let {
            repository.deleteMealPlan(it)
        }
    }
}
