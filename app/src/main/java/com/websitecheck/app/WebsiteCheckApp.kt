package com.websitecheck.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WebsiteCheckApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastReconcileAt = 0L

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scheduler.ensureWatchdog()
        appScope.launch {
            val preferences = getSharedPreferences(SCHEDULER_PREFERENCES, MODE_PRIVATE)
            val scheduleVersion = preferences.getInt(KEY_SCHEDULE_VERSION, 0)
            val result = runCatching {
                container.scheduler.reconcileSchedules(
                    forceReschedule = scheduleVersion < CURRENT_SCHEDULE_VERSION,
                )
            }
            if (result.isSuccess && scheduleVersion < CURRENT_SCHEDULE_VERSION) {
                preferences.edit()
                    .putInt(KEY_SCHEDULE_VERSION, CURRENT_SCHEDULE_VERSION)
                    .apply()
            }
            runCatching { cleanupExpiredLogs() }
        }
    }

    fun reconcileSchedulesAsync() {
        val now = System.currentTimeMillis()
        if (now - lastReconcileAt < RECONCILE_THROTTLE_MILLIS) return
        lastReconcileAt = now
        appScope.launch {
            runCatching { container.scheduler.reconcileSchedules() }
        }
    }

    private suspend fun cleanupExpiredLogs() {
        val retentionDays = container.settingsRepository.settings.value.retentionDays
        if (retentionDays <= 0) return
        val cutoff = System.currentTimeMillis() - retentionDays * DAY_MILLIS
        container.siteRepository.deleteLogsOlderThan(cutoff)
    }

    companion object {
        private const val SCHEDULER_PREFERENCES = "scheduler_migrations"
        private const val KEY_SCHEDULE_VERSION = "schedule_version"
        private const val CURRENT_SCHEDULE_VERSION = 1
        private const val RECONCILE_THROTTLE_MILLIS = 60_000L
        private const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
