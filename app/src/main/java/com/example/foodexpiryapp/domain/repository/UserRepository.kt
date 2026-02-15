package com.example.foodexpiryapp.domain.repository

import com.example.foodexpiryapp.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing user profile and preferences.
 */
interface UserRepository {
    /** Returns the current user profile as a stream. */
    fun getUserProfile(): Flow<UserProfile>

    /** Saves the user profile. */
    suspend fun saveUserProfile(profile: UserProfile)
}
