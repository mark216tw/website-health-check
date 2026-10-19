package com.websitecheck.app.ui.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.websitecheck.app.data.LogWithSite
import com.websitecheck.app.network.errorTypeLabel
import com.websitecheck.app.ui.components.EmptyState
import com.websitecheck.app.ui.components.ScreenHeader
import com.websitecheck.app.ui.components.latencyColor
import com.websitecheck.app.ui.formatFullTimestamp
import com.websitecheck.app.ui.theme.SuccessColor
import java.time.LocalDate

@Composable
fun LogScreen() {
    val viewModel: LogViewModel = viewModel()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val siteFilter by viewModel.siteFilter.collectAsStateWithLifecycle()
    val resultFilter by viewModel.resultFilter.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var siteMenuExpanded by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<LogWithSite?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedSiteName = sites.firstOrNull { it.id == siteFilter }?.name ?: "全部網站"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportFilteredCsv(uri, context.contentResolver)
        }
    }

    LaunchedEffect(viewModel.message) {
        val message = viewModel.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "檢查紀錄",
                subtitle = if (logs.isEmpty()) "目前沒有符合條件的紀錄" else "顯示 ${logs.size} 筆紀錄",
                actions = {
                    IconButton(
                        onClick = {
                            exportLauncher.launch(
                                "website-check-logs-filtered-${LocalDate.now()}.csv",
                            )
                        },
                        enabled = logs.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "匯出目前篩選",
                        )
                    }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResultFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = resultFilter == filter,
                        onClick = { viewModel.selectResult(filter) },
                        label = { Text(filter.label) },
                    )
                }

                Box {
                    OutlinedButton(onClick = { siteMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(selectedSiteName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(
                        expanded = siteMenuExpanded,
                        onDismissRequest = { siteMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部網站") },
                            onClick = {
                                viewModel.selectSite(null)
                                siteMenuExpanded = false
                            },
                        )
                        sites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.name) },
                                onClick = {
                                    viewModel.selectSite(site.id)
                                    siteMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "沒有檢查紀錄",
                    description = "網站執行檢查後，結果會顯示在這裡",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(logs, key = { it.id }) { log ->
                        LogCard(log = log, onClick = { selectedLog = log })
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    selectedLog?.let { log ->
        LogDetailDialog(log = log, onDismiss = { selectedLog = null })
    }
}

@Composable
private fun LogCard(log: LogWithSite, onClick: () -> Unit) {
    val resultColor = if (log.success) SuccessColor else MaterialTheme.colorScheme.error
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = resultColor,
                    modifier = Modifier.size(9.dp),
                ) {}
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = log.siteName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatFullTimestamp(log.checkedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (log.success) "成功" else "失敗",
                    style = MaterialTheme.typography.labelLarge,
                    color = resultColor,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildAnnotatedString {
                    append("HTTP ${log.httpStatus ?: "-"}　回應時間 ")
                    withStyle(
                        SpanStyle(
                            color = latencyColor(log.latencyMs),
                            fontWeight = FontWeight.Medium,
                        ),
                    ) {
                        append("${log.latencyMs} ms")
                    }
                    if (log.errorType != null) append("　${errorTypeLabel(log.errorType)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val errorMessage = log.errorMessage
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LogDetailDialog(log: LogWithSite, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val detailText = buildDetailText(log)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("檢查詳情") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailRow("網站", log.siteName)
                DetailRow("檢查時間", formatFullTimestamp(log.checkedAt))
                DetailRow("結果", if (log.success) "成功" else "失敗")
                DetailRow("HTTP 狀態", log.httpStatus?.toString() ?: "-")
                DetailRow("回應時間", "${log.latencyMs} ms")
                if (log.errorType != null) {
                    DetailRow("錯誤類型", errorTypeLabel(log.errorType))
                }
                DetailRow("網址", log.siteUrl)
                if (!log.effectiveUrl.isNullOrBlank()) {
                    DetailRow("最終網址", log.effectiveUrl)
                }
                val errorMessage = log.errorMessage
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = "錯誤訊息",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("檢查詳情", detailText))
                    onDismiss()
                },
            ) {
                Text("複製")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun buildDetailText(log: LogWithSite): String = buildString {
    appendLine("網站：${log.siteName}")
    appendLine("檢查時間：${formatFullTimestamp(log.checkedAt)}")
    appendLine("結果：${if (log.success) "成功" else "失敗"}")
    appendLine("HTTP 狀態：${log.httpStatus ?: "-"}")
    appendLine("回應時間：${log.latencyMs} ms")
    if (log.errorType != null) appendLine("錯誤類型：${errorTypeLabel(log.errorType)}")
    appendLine("網址：${log.siteUrl}")
    if (!log.effectiveUrl.isNullOrBlank()) appendLine("最終網址：${log.effectiveUrl}")
    if (!log.errorMessage.isNullOrBlank()) appendLine("錯誤訊息：${log.errorMessage}")
}
