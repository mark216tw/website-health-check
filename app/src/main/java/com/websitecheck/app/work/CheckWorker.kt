package com.websitecheck.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import com.websitecheck.app.WebsiteCheckApp
import kotlinx.coroutines.CancellationException

class CheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val siteId = inputData.getLong(KEY_SITE_ID, -1L)
        if (siteId <= 0L) return Result.success()

        val container = (applicationContext as WebsiteCheckApp).container
        val site = container.siteRepository.getSite(siteId) ?: return Result.success()
        if (!site.enabled) {
            container.scheduler.cancel(siteId)
            return Result.success()
        }

        var reschedule = true
        try {
            val outcome = container.healthChecker.check(site)
            val updated = container.siteRepository.recordCheck(site, outcome)
                ?: return Result.success()

            val threshold = updated.failureThreshold.coerceAtLeast(1)
            when {
                updated.lastSuccess == false &&
                    updated.consecutiveFailures >= threshold &&
                    !updated.notifiedFailure -> {
                    if (container.notificationHelper.notifyFailure(updated)) {
                        container.siteRepository.setNotifiedFailure(updated.id, true)
                    }
                }

                updated.lastSuccess == true && updated.notifiedFailure -> {
                    container.notificationHelper.notifyRecovery(updated)
                    container.siteRepository.setNotifiedFailure(updated.id, false)
                }
            }

            val cleanupAt = System.currentTimeMillis()
            val retentionDays = container.settingsRepository.settings.value.retentionDays
            if (retentionDays > 0 && container.settingsRepository.isLogCleanupDue(cleanupAt)) {
                val cutoff = cleanupAt - retentionDays * DAY_MILLIS
                container.siteRepository.deleteLogsOlderThan(cutoff)
                container.settingsRepository.markLogsCleaned(cleanupAt)
            }
        } catch (e: CancellationException) {
            reschedule = false
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error checking site $siteId", e)
        } finally {
            if (reschedule && !isStopped) {
                val latest = container.siteRepository.getSite(siteId)
                if (latest != null && latest.enabled) {
                    container.scheduler.scheduleNext(latest, ExistingWorkPolicy.APPEND_OR_REPLACE)
                } else {
                    container.scheduler.cancel(siteId)
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "CheckWorker"
        const val KEY_SITE_ID = "site_id"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
