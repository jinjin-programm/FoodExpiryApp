package com.example.foodexpiryapp.domain.usecase

import com.example.foodexpiryapp.domain.model.AllergenWarning
import com.example.foodexpiryapp.domain.model.FoodAllergen
import com.example.foodexpiryapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CheckAllergenUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(foodName: String): AllergenWarning? {
        if (foodName.isBlank()) return null
        
        val profile = userRepository.getUserProfile().first()
        val normalizedFood = foodName.lowercase().trim()
        
        val matchedPreset = profile.allergens.presetAllergens.filter { allergen ->
            normalizedFood.contains(allergen.displayName.lowercase()) ||
            allergen.displayName.lowercase().contains(normalizedFood)
        }
        
        if (matchedPreset.isNotEmpty()) {
            return AllergenWarning.Preset(matchedPreset)
        }
        
        val matchedCustom = profile.allergens.customAllergens.filter { custom ->
            normalizedFood == custom.lowercase().trim() ||
            normalizedFood.equals(custom.lowercase().trim(), ignoreCase = true)
        }
        
        if (matchedCustom.isNotEmpty()) {
            return AllergenWarning.Custom(matchedCustom)
        }
        
        return null
    }
}