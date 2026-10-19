package com.websitecheck.app.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.websitecheck.app.ui.components.ScreenHeader
import com.websitecheck.app.data.ThemeMode
import com.websitecheck.app.notify.NotificationHelper
import com.websitecheck.app.ui.formatNextRun
import com.websitecheck.app.ui.formatTimestamp
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var batteryUnrestricted by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }
    var notificationsAvailable by remember {
        mutableStateOf(canPostNotifications(context))
    }

    LaunchedEffect(sites) {
        viewModel.refreshScheduleStates()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        batteryUnrestricted =
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        notificationsAvailable = canPostNotifications(context)
        viewModel.refreshScheduleStates()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        val message = viewModel.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    var showClearDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportCsv(uri, context.contentResolver)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsAvailable = granted && canPostNotifications(context)
        viewModel.setNotificationsEnabled(notificationsAvailable)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "設定", subtitle = "顯示模式、通知、紀錄與資料匯出")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsCard(title = "顯示模式") {
                Text(
                    text = "選擇 App 外觀；系統模式會跟隨手機的深色設定。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }

            SettingsCard(title = "通知") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("網站異常與恢復通知", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "連續失敗達門檻時通知，恢復正常時再通知一次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.notificationsEnabled && notificationsAvailable,
                        onCheckedChange = { checked ->
                            val needsPermission = checked &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else if (checked && !canPostNotifications(context)) {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    },
                                )
                            } else {
                                viewModel.setNotificationsEnabled(checked)
                            }
                        },
                    )
                }
                if (!notificationsAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "系統通知目前關閉，請允許通知或開啟網站狀態通知頻道。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("開啟系統通知設定")
                    }
                }
            }

            SettingsCard(title = "紀錄保留") {
                Text(
                    text = "超過保留天數的檢查紀錄會自動刪除。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(7, 30, 90, 365).forEach { days ->
                        FilterChip(
                            selected = settings.retentionDays == days,
                            onClick = { viewModel.setRetentionDays(days) },
                            label = { Text("$days 天") },
                        )
                    }
                }
            }

            SettingsCard(title = "背景執行") {
                Text(
                    text = if (batteryUnrestricted) {
                        "目前：不受電池最佳化限制。"
                    } else {
                        "目前：受電池最佳化限制，螢幕關閉後檢查可能延遲。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (batteryUnrestricted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                            )
                        }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:${context.packageName}".toUri(),
                                    ),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("開啟電池最佳化設定")
                }
            }

            SettingsCard(title = "排程診斷") {
                Text(
                    text = "確認各網站的自動檢查是否已排入系統。WorkManager 仍可能受省電機制延遲。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.refreshScheduleStates() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("重新整理")
                    }
                    Button(
                        onClick = { viewModel.rescheduleAll() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("重新排程")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val enabledSites = sites.filter { it.enabled }
                if (enabledSites.isEmpty()) {
                    Text(
                        text = "目前沒有啟用的網站",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    enabledSites.forEachIndexed { index, site ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Text(
                            text = site.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "下次：${formatNextRun(site)}　狀態：${
                                viewModel.scheduleStates[site.id] ?: "查詢中"
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "上次：${formatTimestamp(site.lastCheckAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SettingsCard(title = "資料") {
                Button(
                    onClick = {
                        exportLauncher.launch("website-check-logs-${LocalDate.now()}.csv")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("匯出 CSV 檢查紀錄")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("清除所有檢查紀錄", color = MaterialTheme.colorScheme.error)
                }
            }

            SettingsCard(title = "關於背景檢查") {
                Text(
                    text = "儲存或啟用網站後，若目前在檢查時段內，會立即檢查一次；之後依上次檢查時間加上間隔排程。Android 的省電機制（Doze）仍可能延遲背景檢查，若手機長時間未使用或開啟省電模式，實際檢查時間可能晚於排定時間。檢查間隔下限為 15 分鐘。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除所有紀錄？") },
            text = { Text("所有網站設定會保留，但檢查歷史紀錄將無法復原。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearLogs()
                    },
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

private fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
    val channel = context.getSystemService(NotificationManager::class.java)
        .getNotificationChannel(NotificationHelper.CHANNEL_ID)
    return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
