package com.websitecheck.app.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.websitecheck.app.WebsiteCheckApp

class WatchdogWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? WebsiteCheckApp ?: return Result.success()
        return runCatching {
            app.container.scheduler.reconcileSchedules()
            app.container.scheduler.markWatchdogRun()
            Result.success()
        }.getOrElse { error ->
            Log.e(TAG, "Watchdog reconciliation failed", error)
            Result.success()
        }
    }

    companion object {
        const val UNIQUE_NAME = "schedule_watchdog"
        private const val TAG = "WatchdogWorker"
    }
}
