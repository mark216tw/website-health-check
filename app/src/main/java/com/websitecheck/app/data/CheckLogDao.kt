package com.websitecheck.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckLogDao {
    @Query(
        """
        SELECT l.id, l.siteId, l.checkedAt, l.success, l.httpStatus, l.latencyMs,
               l.errorType, l.errorMessage, l.effectiveUrl,
               s.name AS siteName, s.url AS siteUrl
        FROM check_logs l
        INNER JOIN sites s ON s.id = l.siteId
        WHERE (:siteId IS NULL OR l.siteId = :siteId)
          AND (:success IS NULL OR l.success = :success)
        ORDER BY l.checkedAt DESC
        LIMIT 500
        """,
    )
    fun observeRecent(siteId: Long?, success: Boolean?): Flow<List<LogWithSite>>

    @Query(
        """
        SELECT l.id, l.siteId, l.checkedAt, l.success, l.httpStatus, l.latencyMs,
               l.errorType, l.errorMessage, l.effectiveUrl,
               s.name AS siteName, s.url AS siteUrl
        FROM check_logs l
        INNER JOIN sites s ON s.id = l.siteId
        ORDER BY l.checkedAt DESC
        """,
    )
    suspend fun getAllWithSite(): List<LogWithSite>

    @Insert
    suspend fun insert(log: CheckLogEntity): Long

    @Query("DELETE FROM check_logs WHERE checkedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM check_logs")
    suspend fun deleteAll()
}
