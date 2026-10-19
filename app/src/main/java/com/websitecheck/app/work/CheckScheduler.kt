package com.websitecheck.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.websitecheck.app.data.SiteEntity
import com.websitecheck.app.data.SiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class CheckScheduler(
    private val context: Context,
    private val repository: SiteRepository,
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun reconcileSchedules(forceReschedule: Boolean = false) {
        repository.getEnabledSites().forEach { site ->
            val state = workState(site.id)
            if (state == WorkInfo.State.RUNNING) return@forEach
            if (forceReschedule || (state != WorkInfo.State.ENQUEUED && state != WorkInfo.State.RUNNING)) {
                scheduleNext(site, ExistingWorkPolicy.REPLACE)
            }
        }
    }

    suspend fun workState(siteId: Long): WorkInfo.State? = withContext(Dispatchers.IO) {
        val infos = runCatching {
            workManager.getWorkInfosForUniqueWork(uniqueName(siteId)).get()
        }.getOrDefault(emptyList())
        infos.firstOrNull {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }?.state ?: infos.firstOrNull()?.state
    }

    fun reschedule(site: SiteEntity, runImmediately: Boolean = false) {
        if (site.enabled) {
            scheduleNext(site, ExistingWorkPolicy.REPLACE, forceImmediate = runImmediately)
        } else {
            cancel(site.id)
        }
    }

    fun scheduleNext(
        site: SiteEntity,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
        forceImmediate: Boolean = false,
    ) {
        if (!site.enabled) {
            cancel(site.id)
            return
        }
        val nowMillis = System.currentTimeMillis()
        val nextRunMillis = ScheduleCalculator.nextRunMillis(
            site = site,
            nowMillis = nowMillis,
            zone = ZoneId.systemDefault(),
            forceImmediate = forceImmediate,
        ) ?: return
        val delayMillis = ScheduleCalculator.initialDelayMillis(nextRunMillis, nowMillis)

        val request = buildRequest(site.id, delayMillis)
        workManager.enqueueUniqueWork(uniqueName(site.id), policy, request)
    }

    fun runNow(siteId: Long) {
        val request = buildRequest(siteId, 0L)
        workManager.enqueueUniqueWork(uniqueName(siteId), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(siteId: Long) {
        workManager.cancelUniqueWork(uniqueName(siteId))
    }

    private fun buildRequest(siteId: Long, delayMs: Long): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<CheckWorker>()
            .setInputData(workDataOf(CheckWorker.KEY_SITE_ID to siteId))
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .addTag(TAG)
            .build()

    companion object {
        const val TAG = "website_health_check"

        fun uniqueName(siteId: Long): String = "check_site_$siteId"
    }
}
