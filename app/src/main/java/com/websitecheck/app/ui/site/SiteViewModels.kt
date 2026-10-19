package com.websitecheck.app.ui.site

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.websitecheck.app.WebsiteCheckApp
import com.websitecheck.app.data.SiteEntity
import com.websitecheck.app.data.SiteSortOption
import com.websitecheck.app.network.HealthChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SiteListViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as WebsiteCheckApp).container
    private val repository = container.siteRepository
    private val settingsRepository = container.settingsRepository

    val sortOption = settingsRepository.settings
        .map { it.siteSort }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.settings.value.siteSort,
        )

    val sites = combine(repository.sites, sortOption) { sites, option ->
        sites.sortedWith(option.comparator())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _checkingIds = MutableStateFlow<Set<Long>>(emptySet())
    val checkingIds: StateFlow<Set<Long>> = _checkingIds.asStateFlow()

    private val checkStartedAt = mutableMapOf<Long, Long>()

    init {
        viewModelScope.launch {
            repository.sites.collect { currentSites ->
                val checking = _checkingIds.value
                if (checking.isEmpty()) return@collect
                val stillChecking = mutableSetOf<Long>()
                checking.forEach { siteId ->
                    val site = currentSites.firstOrNull { it.id == siteId }
                    val startedAt = checkStartedAt[siteId] ?: 0L
                    val completed = site == null ||
                        !site.enabled ||
                        (site.lastCheckAt ?: 0L) >= startedAt
                    if (completed) {
                        checkStartedAt.remove(siteId)
                    } else {
                        stillChecking += siteId
                    }
                }
                if (stillChecking != checking) {
                    _checkingIds.value = stillChecking
                }
            }
        }
    }

    fun setSortOption(option: SiteSortOption) {
        settingsRepository.setSiteSort(option)
    }

    fun setEnabled(site: SiteEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(site.id, enabled)
            val updated = repository.getSite(site.id) ?: return@launch
            if (updated.enabled) {
                container.scheduler.reschedule(updated, runImmediately = true)
            } else {
                container.scheduler.cancel(updated.id)
            }
        }
    }

    fun runNow(site: SiteEntity) {
        val startedAt = System.currentTimeMillis()
        checkStartedAt[site.id] = startedAt
        _checkingIds.update { it + site.id }
        container.scheduler.runNow(site.id)

        viewModelScope.launch {
            delay(90_000)
            if (checkStartedAt[site.id] == startedAt) {
                val latest = repository.getSite(site.id)
                if (latest == null || (latest.lastCheckAt ?: 0L) < startedAt) {
                    _checkingIds.update { it - site.id }
                    checkStartedAt.remove(site.id)
                }
            }
        }
    }
}

data class SiteFormState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val url: String = "",
    val method: String = "HEAD",
    val timeoutSeconds: String = "10",
    val expectedStatusMin: String = "200",
    val expectedStatusMax: String = "399",
    val followRedirects: Boolean = true,
    val keywordCheckEnabled: Boolean = false,
    val expectedKeyword: String = "",
    val failureThreshold: String = "3",
    val enabled: Boolean = true,
    val days: Set<Int> = (0..6).toSet(),
    val windowStartMinutes: Int = 0,
    val windowEndMinutes: Int = 23 * 60 + 59,
    val intervalMinutes: String = "60",
    val saving: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null,
    val duplicateWarning: String? = null,
)

class SiteEditViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as WebsiteCheckApp).container
    private val repository = container.siteRepository

    var uiState by mutableStateOf(SiteFormState())
        private set

    private var loadedId: Long? = null
    private var original: SiteEntity? = null

    fun load(siteId: Long) {
        if (loadedId == siteId) return
        loadedId = siteId
        if (siteId <= 0L) {
            uiState = SiteFormState(loaded = true, isNew = true)
            return
        }
        viewModelScope.launch {
            val site = repository.getSite(siteId)
            if (site == null) {
                uiState = SiteFormState(loaded = true, error = "找不到這個網站")
                return@launch
            }
            original = site
            uiState = SiteFormState(
                loaded = true,
                isNew = false,
                name = site.name,
                url = site.url,
                method = if (site.keywordCheckEnabled) "GET" else site.method,
                timeoutSeconds = (site.timeoutMs / 1_000).toString(),
                expectedStatusMin = site.expectedStatusMin.toString(),
                expectedStatusMax = site.expectedStatusMax.toString(),
                followRedirects = site.followRedirects,
                keywordCheckEnabled = site.keywordCheckEnabled,
                expectedKeyword = site.expectedKeyword,
                failureThreshold = site.failureThreshold.toString(),
                enabled = site.enabled,
                days = (0..6).filter { site.daysOfWeek and (1 shl it) != 0 }.toSet(),
                windowStartMinutes = site.windowStartMinutes,
                windowEndMinutes = site.windowEndMinutes,
                intervalMinutes = site.intervalMinutes.toString(),
            )
        }
    }

    fun updateForm(transform: (SiteFormState) -> SiteFormState) {
        uiState = transform(uiState)
    }

    fun save(ignoreDuplicate: Boolean = false) {
        val state = uiState
        val name = state.name.trim()
        val timeout = state.timeoutSeconds.toIntOrNull()
        val statusMin = state.expectedStatusMin.toIntOrNull()
        val statusMax = state.expectedStatusMax.toIntOrNull()
        val threshold = state.failureThreshold.toIntOrNull()
        val interval = state.intervalMinutes.toIntOrNull()

        val error = when {
            name.isBlank() -> "請輸入網站名稱"
            !HealthChecker.isValidUrl(state.url) -> "請輸入有效的網址，例如 https://example.com"
            timeout == null || timeout !in 1..60 -> "逾時秒數需介於 1 到 60"
            statusMin == null || statusMin !in 100..599 -> "預期狀態碼下限需介於 100 到 599"
            statusMax == null || statusMax !in 100..599 -> "預期狀態碼上限需介於 100 到 599"
            statusMin > statusMax -> "狀態碼下限不可大於上限"
            state.keywordCheckEnabled && state.expectedKeyword.isBlank() -> "請輸入要檢查的內容字串"
            state.keywordCheckEnabled && state.expectedKeyword.length > 200 -> "內容字串不可超過 200 字"
            threshold == null || threshold !in 1..10 -> "失敗通知門檻需介於 1 到 10"
            interval == null || interval < 15 -> "檢查間隔至少 15 分鐘"
            interval > 1_440 -> "檢查間隔不可超過 1440 分鐘"
            state.days.isEmpty() -> "請至少選擇一個檢查日"
            else -> null
        }

        if (error != null) {
            uiState = state.copy(error = error)
            return
        }

        val timeoutValue = timeout ?: return
        val statusMinValue = statusMin ?: return
        val statusMaxValue = statusMax ?: return
        val thresholdValue = threshold ?: return
        val intervalValue = interval ?: return

        uiState = state.copy(saving = true, error = null, duplicateWarning = null)
        viewModelScope.launch {
            val base = original
            val url = HealthChecker.normalizeUrl(state.url)

            if (!ignoreDuplicate) {
                val canonical = HealthChecker.canonicalUrl(url)
                val duplicate = repository.getAllSites().firstOrNull { site ->
                    site.id != (base?.id ?: 0L) &&
                        HealthChecker.canonicalUrl(site.url) == canonical
                }
                if (duplicate != null) {
                    uiState = uiState.copy(
                        saving = false,
                        duplicateWarning = duplicate.name,
                    )
                    return@launch
                }
            }

            val daysMask = state.days.fold(0) { mask, day -> mask or (1 shl day) }
            val entity = SiteEntity(
                id = base?.id ?: 0L,
                name = name,
                url = url,
                enabled = state.enabled,
                method = if (state.keywordCheckEnabled || state.method == "GET") "GET" else "HEAD",
                timeoutMs = timeoutValue * 1_000,
                expectedStatusMin = statusMinValue,
                expectedStatusMax = statusMaxValue,
                followRedirects = state.followRedirects,
                keywordCheckEnabled = state.keywordCheckEnabled,
                expectedKeyword = state.expectedKeyword,
                failureThreshold = thresholdValue,
                daysOfWeek = daysMask,
                windowStartMinutes = state.windowStartMinutes.coerceIn(0, 24 * 60),
                windowEndMinutes = state.windowEndMinutes.coerceIn(0, 24 * 60),
                intervalMinutes = intervalValue,
                createdAt = base?.createdAt ?: System.currentTimeMillis(),
                lastCheckAt = base?.lastCheckAt,
                lastSuccess = base?.lastSuccess,
                lastLatencyMs = base?.lastLatencyMs,
                lastStatus = base?.lastStatus,
                lastError = base?.lastError,
                consecutiveFailures = base?.consecutiveFailures ?: 0,
                notifiedFailure = base?.notifiedFailure ?: false,
            )
            val id = repository.save(entity)
            val saved = repository.getSite(id)
            if (saved != null) {
                if (saved.enabled) {
                    container.scheduler.reschedule(saved, runImmediately = true)
                } else {
                    container.scheduler.cancel(saved.id)
                }
            }
            uiState = uiState.copy(saving = false, finished = true, duplicateWarning = null)
        }
    }

    fun dismissDuplicateWarning() {
        uiState = uiState.copy(duplicateWarning = null)
    }

    fun delete() {
        val id = original?.id ?: return
        viewModelScope.launch {
            container.scheduler.cancel(id)
            repository.delete(id)
            uiState = uiState.copy(finished = true)
        }
    }
}
