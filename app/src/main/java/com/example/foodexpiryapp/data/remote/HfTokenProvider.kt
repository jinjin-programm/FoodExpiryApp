package com.example.foodexpiryapp.data.remote

/**
 * Provides HuggingFace authentication token for private repo access.
 * Abstracted so the implementation can be swapped (BuildConfig, DataStore, etc.)
 */
interface HfTokenProvider {
    /**
     * Returns the HuggingFace access token, or empty string if not configured.
     */
    fun getToken(): String
}
