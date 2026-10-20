package com.websitecheck.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
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
    private val statePreferences = context.applicationContext
        .getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)

    suspend fun reconcileSchedules(forceReschedule: Boolean = false) {
        ensureWatchdog()
        val nowMillis = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        repository.getEnabledSites().forEach { site ->
            val state = workState(site.id)
            if (state == WorkInfo.State.RUNNING) return@forEach
            when {
                OverdueEvaluator.isOverdue(site, nowMillis, zone) ->
                    scheduleNext(site, ExistingWorkPolicy.REPLACE, forceImmediate = true)

                forceReschedule ->
                    scheduleNext(site, ExistingWorkPolicy.REPLACE)

                state != WorkInfo.State.ENQUEUED ->
                    scheduleNext(site, ExistingWorkPolicy.REPLACE)
            }
        }
    }

    fun ensureWatchdog() {
        val request = PeriodicWorkRequestBuilder<WatchdogWorker>(
            WATCHDOG_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .addTag(WATCHDOG_TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WatchdogWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun markWatchdogRun(nowMillis: Long = System.currentTimeMillis()) {
        statePreferences.edit().putLong(KEY_LAST_WATCHDOG_RUN_AT, nowMillis).apply()
    }

    fun lastWatchdogRunAt(): Long = statePreferences.getLong(KEY_LAST_WATCHDOG_RUN_AT, 0L)

    suspend fun workState(siteId: Long): WorkInfo.State? = stateOf(uniqueName(siteId))

    suspend fun watchdogState(): WorkInfo.State? = stateOf(WatchdogWorker.UNIQUE_NAME)

    private suspend fun stateOf(uniqueWorkName: String): WorkInfo.State? =
        withContext(Dispatchers.IO) {
            val infos = runCatching {
                workManager.getWorkInfosForUniqueWork(uniqueWorkName).get()
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

        private const val WATCHDOG_TAG = "schedule_watchdog"
        private const val WATCHDOG_INTERVAL_MINUTES = 15L
        private const val STATE_PREFERENCES = "scheduler_state"
        private const val KEY_LAST_WATCHDOG_RUN_AT = "last_watchdog_run_at"

        fun uniqueName(siteId: Long): String = "check_site_$siteId"
    }
}
