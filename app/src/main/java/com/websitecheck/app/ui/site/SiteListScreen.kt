package com.websitecheck.app.ui.site

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.websitecheck.app.data.SiteEntity
import com.websitecheck.app.data.SiteSortOption
import com.websitecheck.app.ui.components.EmptyState
import com.websitecheck.app.ui.components.ScreenHeader
import com.websitecheck.app.ui.components.latencyColor
import com.websitecheck.app.ui.formatLatency
import com.websitecheck.app.ui.formatNextRun
import com.websitecheck.app.ui.formatSchedule
import com.websitecheck.app.ui.formatTimestamp
import com.websitecheck.app.ui.theme.SuccessColor

@Composable
fun SiteListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel: SiteListViewModel = viewModel()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val checkingIds by viewModel.checkingIds.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    var showSortDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "網站健康檢查",
                subtitle = if (sites.isEmpty()) {
                    "尚未新增網站"
                } else {
                    "共 ${sites.size} 個網站・${sortOption.label}"
                },
                actions = {
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = "排序",
                        )
                    }
                },
            )
            if (sites.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Language,
                    title = "沒有網站",
                    description = "點擊右下角按鈕新增第一個要監控的網址",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sites, key = { it.id }) { site ->
                        SiteCard(
                            site = site,
                            now = now,
                            checking = site.id in checkingIds,
                            onEdit = { onEdit(site.id) },
                            onRunNow = { viewModel.runNow(site) },
                            onToggleEnabled = { enabled -> viewModel.setEnabled(site, enabled) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "新增網站")
        }
    }

    if (showSortDialog) {
        SortDialog(
            initial = sortOption,
            onDismiss = { showSortDialog = false },
            onConfirm = { option ->
                viewModel.setSortOption(option)
                showSortDialog = false
            },
        )
    }
}

@Composable
private fun SortDialog(
    initial: SiteSortOption,
    onDismiss: () -> Unit,
    onConfirm: (SiteSortOption) -> Unit,
) {
    var selected by remember(initial) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序網站") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                SiteSortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == option,
                                onClick = { selected = option },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == option, onClick = null)
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("確認")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun SiteCard(
    site: SiteEntity,
    now: Long,
    checking: Boolean,
    onEdit: () -> Unit,
    onRunNow: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
) {
    val statusColor = when {
        !site.enabled -> MaterialTheme.colorScheme.outline
        site.lastSuccess == null -> MaterialTheme.colorScheme.secondary
        site.lastSuccess == true -> SuccessColor
        else -> MaterialTheme.colorScheme.error
    }
    val statusLabel = when {
        !site.enabled -> "已停用"
        site.lastSuccess == null -> "尚未檢查"
        site.lastSuccess == true -> "正常"
        else -> "異常"
    }
    val displayColor = if (checking) MaterialTheme.colorScheme.primary else statusColor
    val displayLabel = if (checking) "檢查中" else statusLabel

    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (site.enabled) 1f else 0.65f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = displayColor,
                    modifier = Modifier.size(10.dp),
                ) {}
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = site.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (checking) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    IconButton(onClick = onRunNow, enabled = site.enabled) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "立即檢查",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Switch(checked = site.enabled, onCheckedChange = onToggleEnabled)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = displayColor.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = displayColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "回應時間 ${formatLatency(site.lastLatencyMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = latencyColor(site.lastLatencyMs),
                )
                if (site.consecutiveFailures > 0) {
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "連續失敗 ${site.consecutiveFailures} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "上次：${formatTimestamp(site.lastCheckAt)}　下次：${formatNextRun(site, now)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatSchedule(site),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
