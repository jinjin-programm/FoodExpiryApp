package com.example.foodexpiryapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodexpiryapp.data.local.database.DownloadStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [DownloadStateEntity].
 * Per DL-07: Provides persistence for download state across app restarts.
 *
 * Follows existing DAO pattern (see FoodItemDao): Flow for reactive queries,
 * suspend functions for one-shot operations.
 */
@Dao
interface DownloadStateDao {

    @Query("SELECT * FROM download_state WHERE filePath = :filePath")
    suspend fun getDownloadState(filePath: String): DownloadStateEntity?

    @Query("SELECT * FROM download_state ORDER BY filePath")
    fun getAllDownloadStates(): Flow<List<DownloadStateEntity>>

    @Query("SELECT * FROM download_state WHERE status = :status ORDER BY filePath")
    fun getDownloadStatesByStatus(status: String): Flow<List<DownloadStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadState(entity: DownloadStateEntity)

    @Query("""
        UPDATE download_state
        SET downloadedBytes = :bytes, totalBytes = :totalBytes, status = :status, updatedAt = :timestamp
        WHERE filePath = :filePath
    """)
    suspend fun updateProgress(
        filePath: String,
        bytes: Long,
        totalBytes: Long = 0L,
        status: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE download_state
        SET eTag = :eTag, lastModified = :lastModified, updatedAt = :timestamp
        WHERE filePath = :filePath
    """)
    suspend fun updateETag(
        filePath: String,
        eTag: String?,
        lastModified: String?,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE download_state
        SET status = 'VERIFYING', updatedAt = :timestamp
        WHERE filePath = :filePath
    """)
    suspend fun markVerifying(
        filePath: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE download_state
        SET status = 'COMPLETE', actualSha256 = :sha256, downloadedBytes = totalBytes, updatedAt = :timestamp
        WHERE filePath = :filePath
    """)
    suspend fun markComplete(
        filePath: String,
        sha256: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE download_state
        SET status = 'FAILED', errorMessage = :error, updatedAt = :timestamp
        WHERE filePath = :filePath
    """)
    suspend fun markFailed(
        filePath: String,
        error: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE download_state
        SET status = 'CANCELLED', updatedAt = :timestamp
        WHERE filePath = :filePath
    """)
    suspend fun markCancelled(
        filePath: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM download_state WHERE filePath = :filePath")
    suspend fun deleteDownloadState(filePath: String)

    @Query("DELETE FROM download_state")
    suspend fun deleteAllDownloadStates()

    @Query("SELECT COALESCE(SUM(downloadedBytes), 0) FROM download_state")
    suspend fun getTotalDownloadedBytes(): Long

    @Query("SELECT COALESCE(SUM(totalBytes), 0) FROM download_state")
    suspend fun getTotalBytes(): Long

    @Query("SELECT COUNT(*) FROM download_state WHERE status = 'COMPLETE'")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM download_state WHERE status IN ('PENDING', 'DOWNLOADING')")
    suspend fun getActiveDownloadCount(): Int

    @Query("SELECT COUNT(*) FROM download_state WHERE status = 'FAILED'")
    suspend fun getFailedDownloadCount(): Int
}
