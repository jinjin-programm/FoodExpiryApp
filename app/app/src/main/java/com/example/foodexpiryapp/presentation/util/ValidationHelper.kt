package com.example.foodexpiryapp.presentation.util

/**
 * Utility class for form field validation with real-time error messages
 */
object ValidationHelper {
    
    /**
     * Validates user name field
     * @return error message or null if valid
     */
    fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must be less than 50 characters"
            else -> null
        }
    }

    /**
     * Checks if all validations pass
     */
    fun isNameValid(name: String): Boolean {
        return validateName(name) == null
    }
}
