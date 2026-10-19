package com.websitecheck.app.data

import androidx.room.withTransaction
import com.websitecheck.app.network.CheckOutcome
import kotlinx.coroutines.flow.Flow

class SiteRepository(
    private val database: AppDatabase,
) {
    private val siteDao = database.siteDao()
    private val logDao = database.checkLogDao()

    val sites: Flow<List<SiteEntity>> = siteDao.observeAll()

    fun recentLogs(siteId: Long?, success: Boolean?): Flow<List<LogWithSite>> =
        logDao.observeRecent(siteId, success)

    fun site(id: Long): Flow<SiteEntity?> = siteDao.observeById(id)

    suspend fun getSite(id: Long): SiteEntity? = siteDao.getById(id)

    suspend fun getEnabledSites(): List<SiteEntity> = siteDao.getEnabled()

    suspend fun getAllSites(): List<SiteEntity> = siteDao.getAll()

    suspend fun save(site: SiteEntity): Long = database.withTransaction {
        if (site.id == 0L) {
            siteDao.insert(site)
        } else {
            val latest = siteDao.getById(site.id)
            siteDao.update(if (latest == null) site else site.withCheckStateFrom(latest))
            site.id
        }
    }

    suspend fun delete(id: Long) = siteDao.deleteById(id)

    suspend fun setEnabled(id: Long, enabled: Boolean) = siteDao.setEnabled(id, enabled)

    suspend fun recordCheck(site: SiteEntity, outcome: CheckOutcome): SiteEntity? =
        database.withTransaction {
            val current = siteDao.getById(site.id) ?: return@withTransaction null
            val checkedAt = System.currentTimeMillis()
            logDao.insert(
                CheckLogEntity(
                    siteId = site.id,
                    checkedAt = checkedAt,
                    success = outcome.success,
                    httpStatus = outcome.httpStatus,
                    latencyMs = outcome.latencyMs,
                    errorType = outcome.errorType,
                    errorMessage = outcome.errorMessage,
                    effectiveUrl = outcome.effectiveUrl,
                ),
            )
            val failures = if (outcome.success) 0 else current.consecutiveFailures + 1
            siteDao.updateSummary(
                id = site.id,
                checkedAt = checkedAt,
                success = outcome.success,
                latencyMs = outcome.latencyMs,
                httpStatus = outcome.httpStatus,
                error = outcome.errorMessage,
                consecutiveFailures = failures,
            )
            current.copy(
                lastCheckAt = checkedAt,
                lastSuccess = outcome.success,
                lastLatencyMs = outcome.latencyMs,
                lastStatus = outcome.httpStatus,
                lastError = outcome.errorMessage,
                consecutiveFailures = failures,
            )
        }

    suspend fun setNotifiedFailure(id: Long, notified: Boolean) =
        siteDao.setNotifiedFailure(id, notified)

    suspend fun deleteLogsOlderThan(cutoff: Long) = logDao.deleteOlderThan(cutoff)

    suspend fun clearLogs() = logDao.deleteAll()

    suspend fun getAllLogsWithSite(): List<LogWithSite> = logDao.getAllWithSite()

    private fun SiteEntity.withCheckStateFrom(latest: SiteEntity) = copy(
        lastCheckAt = latest.lastCheckAt,
        lastSuccess = latest.lastSuccess,
        lastLatencyMs = latest.lastLatencyMs,
        lastStatus = latest.lastStatus,
        lastError = latest.lastError,
        consecutiveFailures = latest.consecutiveFailures,
        notifiedFailure = latest.notifiedFailure,
    )
}
