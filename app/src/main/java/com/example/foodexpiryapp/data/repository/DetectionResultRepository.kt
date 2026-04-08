package com.example.foodexpiryapp.data.repository

import com.example.foodexpiryapp.data.local.dao.DetectionResultDao
import com.example.foodexpiryapp.data.local.database.DetectionResultEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing temporary detection results.
 * Per D-17: Detection results stored in temporary Room table to survive process death.
 * Per D-20: ConfirmationFragment reads from DB on recreation.
 *
 * This repository provides a clean API for both the detection pipeline (writing results)
 * and the confirmation UI (reading results after potential process death).
 */
@Singleton
class DetectionResultRepository @Inject constructor(
    private val dao: DetectionResultDao
) {
    /**
     * Save a batch of detection result entities for a given session.
     * Used by the detection pipeline to persist results after YOLO+LLM processing.
     */
    suspend fun insertResults(results: List<DetectionResultEntity>) {
        dao.insertAll(results)
    }

    /**
     * Observe results for a session reactively.
     * Used by ConfirmationFragment to display results via Flow.
     */
    fun getResults(sessionId: String): Flow<List<DetectionResultEntity>> {
        return dao.getBySessionId(sessionId)
    }

    /**
     * Get results for a session synchronously (one-shot).
     * Used by ConfirmationFragment after process recreation to load initial state.
     */
    suspend fun getResultsSync(sessionId: String): List<DetectionResultEntity> {
        return dao.getBySessionIdSync(sessionId)
    }

    /**
     * Update a single detection result (e.g., user edits name or status).
     */
    suspend fun updateResult(result: DetectionResultEntity) {
        dao.update(result)
    }

    /**
     * Delete all results for a session after confirmation is complete.
     */
    suspend fun clearSession(sessionId: String) {
        dao.deleteBySessionId(sessionId)
    }

    /**
     * Cleanup old results to prevent unbounded temp table growth.
     * Per T-06-11: mitigates denial of service from temp table filling storage.
     * Should be called periodically (e.g., on app startup or via WorkManager).
     */
    suspend fun cleanupOldResults() {
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        dao.deleteOlderThan(oneDayAgo)
    }
}