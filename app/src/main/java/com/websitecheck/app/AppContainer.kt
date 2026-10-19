package com.websitecheck.app

import android.content.Context
import com.websitecheck.app.data.AppDatabase
import com.websitecheck.app.data.SettingsRepository
import com.websitecheck.app.data.SiteRepository
import com.websitecheck.app.network.HealthChecker
import com.websitecheck.app.notify.NotificationHelper
import com.websitecheck.app.work.CheckScheduler

class AppContainer(context: Context) {
    private val database = AppDatabase.get(context)

    val settingsRepository = SettingsRepository(context)
    val siteRepository = SiteRepository(database)
    val healthChecker = HealthChecker()
    val notificationHelper = NotificationHelper(context, settingsRepository)
    val scheduler = CheckScheduler(context, siteRepository)
}
