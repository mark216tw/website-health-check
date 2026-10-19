package com.websitecheck.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val LOG_CLEANUP_INTERVAL_MILLIS = 24L * 60L * 60L * 1_000L

internal fun logCleanupIsDue(nowMillis: Long, lastCleanupAt: Long): Boolean =
    lastCleanupAt == 0L || nowMillis < lastCleanupAt ||
        nowMillis - lastCleanupAt >= LOG_CLEANUP_INTERVAL_MILLIS

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val retentionDays: Int = 30,
    val siteSort: SiteSortOption = SiteSortOption.CREATED_ASC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        retentionDays = prefs.getInt(KEY_RETENTION_DAYS, 30),
        siteSort = prefs.getString(KEY_SITE_SORT, null)
            ?.let { stored -> runCatching { SiteSortOption.valueOf(stored) }.getOrNull() }
            ?: SiteSortOption.CREATED_ASC,
        themeMode = prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
    )

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _settings.value = _settings.value.copy(notificationsEnabled = enabled)
    }

    fun setRetentionDays(days: Int) {
        prefs.edit { putInt(KEY_RETENTION_DAYS, days) }
        _settings.value = _settings.value.copy(retentionDays = days)
    }

    fun setSiteSort(option: SiteSortOption) {
        prefs.edit { putString(KEY_SITE_SORT, option.name) }
        _settings.value = _settings.value.copy(siteSort = option)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun isLogCleanupDue(nowMillis: Long): Boolean {
        val lastCleanupAt = prefs.getLong(KEY_LAST_LOG_CLEANUP_AT, 0L)
        return logCleanupIsDue(nowMillis, lastCleanupAt)
    }

    fun markLogsCleaned(nowMillis: Long) {
        prefs.edit { putLong(KEY_LAST_LOG_CLEANUP_AT, nowMillis) }
    }

    companion object {
        private const val PREFS_NAME = "website_check_settings"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_SITE_SORT = "site_sort"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_LOG_CLEANUP_AT = "last_log_cleanup_at"
    }
}
