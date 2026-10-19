package com.websitecheck.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    fun observeById(id: Long): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getById(id: Long): SiteEntity?

    @Query("SELECT * FROM sites WHERE enabled = 1")
    suspend fun getEnabled(): List<SiteEntity>

    @Query("SELECT * FROM sites")
    suspend fun getAll(): List<SiteEntity>

    @Insert
    suspend fun insert(site: SiteEntity): Long

    @Update
    suspend fun update(site: SiteEntity)

    @Delete
    suspend fun delete(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sites SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query(
        """
        UPDATE sites SET
            lastCheckAt = :checkedAt,
            lastSuccess = :success,
            lastLatencyMs = :latencyMs,
            lastStatus = :httpStatus,
            lastError = :error,
            consecutiveFailures = :consecutiveFailures
        WHERE id = :id
        """,
    )
    suspend fun updateSummary(
        id: Long,
        checkedAt: Long,
        success: Boolean,
        latencyMs: Long,
        httpStatus: Int?,
        error: String?,
        consecutiveFailures: Int,
    )

    @Query("UPDATE sites SET notifiedFailure = :notified WHERE id = :id")
    suspend fun setNotifiedFailure(id: Long, notified: Boolean)
}
