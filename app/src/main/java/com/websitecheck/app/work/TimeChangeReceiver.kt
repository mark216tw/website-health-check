package com.websitecheck.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.websitecheck.app.WebsiteCheckApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val app = context.applicationContext as? WebsiteCheckApp ?: return
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        runCatching {
                            app.container.scheduler.reconcileSchedules(forceReschedule = true)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
