package com.websitecheck.app.ui.log

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.websitecheck.app.WebsiteCheckApp
import com.websitecheck.app.data.LogCsv
import com.websitecheck.app.data.LogWithSite
import com.websitecheck.app.data.SiteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

enum class ResultFilter(val label: String) {
    ALL("全部"),
    SUCCESS("成功"),
    FAILURE("失敗"),
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WebsiteCheckApp).container.siteRepository

    val sites = repository.sites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _siteFilter = MutableStateFlow<Long?>(null)
    val siteFilter: StateFlow<Long?> = _siteFilter.asStateFlow()

    private val _resultFilter = MutableStateFlow(ResultFilter.ALL)
    val resultFilter: StateFlow<ResultFilter> = _resultFilter.asStateFlow()

    val logs: StateFlow<List<LogWithSite>> =
        combine(_siteFilter, _resultFilter, sites) { siteId, result, availableSites ->
            val validSiteId = siteId?.takeIf { selectedId ->
                availableSites.any { it.id == selectedId }
            }
            validSiteId to when (result) {
                ResultFilter.ALL -> null
                ResultFilter.SUCCESS -> true
                ResultFilter.FAILURE -> false
            }
        }.flatMapLatest { (siteId, success) ->
            repository.recentLogs(siteId, success)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun selectSite(siteId: Long?) {
        _siteFilter.value = siteId
    }

    fun selectResult(filter: ResultFilter) {
        _resultFilter.value = filter
    }

    var message by mutableStateOf<String?>(null)
        private set

    fun consumeMessage() {
        message = null
    }

    fun exportFilteredCsv(uri: Uri, resolver: ContentResolver) {
        val siteId = _siteFilter.value
        val success = when (_resultFilter.value) {
            ResultFilter.ALL -> null
            ResultFilter.SUCCESS -> true
            ResultFilter.FAILURE -> false
        }
        viewModelScope.launch {
            try {
                val exported = withContext(Dispatchers.IO) {
                    val logs = repository.getAllLogsWithSite().filter { log ->
                        (siteId == null || log.siteId == siteId) &&
                            (success == null || log.success == success)
                    }
                    val csv = LogCsv.build(logs)
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: throw IOException("無法寫入檔案")
                    logs.size
                }
                message = "已匯出 $exported 筆篩選紀錄"
            } catch (e: Exception) {
                message = "匯出失敗：${e.message ?: "未知錯誤"}"
            }
        }
    }

    fun siteName(siteId: Long?, sites: List<SiteEntity>): String =
        sites.firstOrNull { it.id == siteId }?.name ?: "全部網站"
}
