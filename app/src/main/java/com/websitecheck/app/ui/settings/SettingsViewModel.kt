package com.websitecheck.app.ui.settings

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.websitecheck.app.WebsiteCheckApp
import com.websitecheck.app.data.LogCsv
import com.websitecheck.app.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as WebsiteCheckApp).container
    private val repository = container.siteRepository
    private val settingsRepository = container.settingsRepository

    val settings = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = settingsRepository.settings.value,
    )

    val sites = repository.sites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    var scheduleStates by mutableStateOf<Map<Long, String>>(emptyMap())
        private set

    var message by mutableStateOf<String?>(null)
        private set

    fun refreshScheduleStates() {
        viewModelScope.launch {
            val enabledSites = repository.getEnabledSites()
            val states = mutableMapOf<Long, String>()
            enabledSites.forEach { site ->
                states[site.id] = scheduleStateLabel(container.scheduler.workState(site.id))
            }
            scheduleStates = states
        }
    }

    fun rescheduleAll() {
        viewModelScope.launch {
            val enabledSites = repository.getEnabledSites()
            enabledSites.forEach { site -> container.scheduler.reschedule(site) }
            message = "已重新排程 ${enabledSites.size} 個網站"
            delay(300)
            refreshScheduleStates()
        }
    }

    private fun scheduleStateLabel(state: WorkInfo.State?): String = when (state) {
        null -> "未排程"
        WorkInfo.State.ENQUEUED -> "已排入"
        WorkInfo.State.RUNNING -> "執行中"
        WorkInfo.State.BLOCKED -> "等待中"
        WorkInfo.State.SUCCEEDED -> "已完成（待重新排程）"
        WorkInfo.State.FAILED -> "失敗（待重新排程）"
        WorkInfo.State.CANCELLED -> "已取消（待重新排程）"
    }

    fun setNotificationsEnabled(enabled: Boolean) = settingsRepository.setNotificationsEnabled(enabled)

    fun setRetentionDays(days: Int) {
        settingsRepository.setRetentionDays(days)
        viewModelScope.launch {
            if (days > 0) {
                val now = System.currentTimeMillis()
                val cutoff = now - days * 24L * 60L * 60L * 1_000L
                repository.deleteLogsOlderThan(cutoff)
                settingsRepository.markLogsCleaned(now)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    fun exportCsv(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val exportedCount = withContext(Dispatchers.IO) {
                    val logs = repository.getAllLogsWithSite()
                    val csv = LogCsv.build(logs)
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: throw IOException("無法寫入檔案")
                    logs.size
                }
                message = "已匯出 $exportedCount 筆檢查紀錄"
            } catch (e: Exception) {
                message = "匯出失敗：${e.message ?: "未知錯誤"}"
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            message = "已清除所有檢查紀錄"
        }
    }

    fun consumeMessage() {
        message = null
    }
}
