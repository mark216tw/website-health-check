package com.websitecheck.app.ui.site

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.websitecheck.app.ui.components.ScreenHeader
import com.websitecheck.app.ui.formatMinutesOfDay
import com.websitecheck.app.ui.theme.WarningColor

private val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun SiteEditScreen(siteId: Long, onDone: () -> Unit) {
    val viewModel: SiteEditViewModel = viewModel()
    val state = viewModel.uiState

    LaunchedEffect(siteId) { viewModel.load(siteId) }
    LaunchedEffect(state.finished) { if (state.finished) onDone() }

    if (!state.loaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (state.isNew) "新增網站" else "編輯網站",
            subtitle = if (state.isNew) "設定網址與自動檢查排程" else "修改網址或檢查排程",
            onBack = onDone,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { value -> viewModel.updateForm { it.copy(name = value) } },
                label = { Text("網站名稱") },
                placeholder = { Text("例如：公司官網") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = { value -> viewModel.updateForm { it.copy(url = value) } },
                label = { Text("網址") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                supportingText = if (state.url.trim().startsWith("http://", ignoreCase = true)) {
                    { Text("使用 HTTP，內容未加密", color = WarningColor) }
                } else {
                    null
                },
            )

            Text("HTTP 方法", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.method == "HEAD",
                    onClick = { viewModel.updateForm { it.copy(method = "HEAD") } },
                    label = { Text("HEAD（較省流量）") },
                    enabled = !state.keywordCheckEnabled,
                )
                FilterChip(
                    selected = state.method == "GET",
                    onClick = { viewModel.updateForm { it.copy(method = "GET") } },
                    label = { Text("GET") },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.timeoutSeconds,
                    onValueChange = { value ->
                        viewModel.updateForm { it.copy(timeoutSeconds = value.filter(Char::isDigit)) }
                    },
                    label = { Text("逾時（秒）") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.failureThreshold,
                    onValueChange = { value ->
                        viewModel.updateForm { it.copy(failureThreshold = value.filter(Char::isDigit)) }
                    },
                    label = { Text("失敗通知門檻") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.expectedStatusMin,
                    onValueChange = { value ->
                        viewModel.updateForm { it.copy(expectedStatusMin = value.filter(Char::isDigit)) }
                    },
                    label = { Text("狀態碼下限") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = state.expectedStatusMax,
                    onValueChange = { value ->
                        viewModel.updateForm { it.copy(expectedStatusMax = value.filter(Char::isDigit)) }
                    },
                    label = { Text("狀態碼上限") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }

            SwitchRow(
                title = "跟隨轉址",
                description = "遇到 3xx 時自動前往最終網址",
                checked = state.followRedirects,
                onCheckedChange = { checked -> viewModel.updateForm { it.copy(followRedirects = checked) } },
            )

            Text("內容檢查", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SwitchRow(
                title = "檢查內容包含字串",
                description = "下載頁面內容並確認包含指定字串（自動使用 GET）",
                checked = state.keywordCheckEnabled,
                onCheckedChange = { checked ->
                    viewModel.updateForm {
                        it.copy(
                            keywordCheckEnabled = checked,
                            method = if (checked) "GET" else it.method,
                        )
                    }
                },
            )
            if (state.keywordCheckEnabled) {
                OutlinedTextField(
                    value = state.expectedKeyword,
                    onValueChange = { value ->
                        viewModel.updateForm { it.copy(expectedKeyword = value) }
                    },
                    label = { Text("預期包含的字串") },
                    placeholder = { Text("例如：系統正常運作") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                Text(
                    text = "最多讀取前 1 MB 的原始回應內容，比對區分大小寫。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SwitchRow(
                title = "啟用自動檢查",
                description = "關閉後仍保留設定與歷史紀錄",
                checked = state.enabled,
                onCheckedChange = { checked -> viewModel.updateForm { it.copy(enabled = checked) } },
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text("檢查排程", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "選擇檢查星期、每日時間區段與檢查頻率。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("檢查星期", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                dayLabels.forEachIndexed { index, label ->
                    val selected = index in state.days
                    Surface(
                        onClick = {
                            viewModel.updateForm { form ->
                                form.copy(
                                    days = if (index in form.days) form.days - index else form.days + index,
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }

            Text("每日時間區段", style = MaterialTheme.typography.titleSmall)
            val context = LocalContext.current
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        showTimePicker(context, state.windowStartMinutes) { picked ->
                            viewModel.updateForm { it.copy(windowStartMinutes = picked) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("開始 ${formatMinutesOfDay(state.windowStartMinutes)}")
                }
                OutlinedButton(
                    onClick = {
                        showTimePicker(context, state.windowEndMinutes) { picked ->
                            viewModel.updateForm { it.copy(windowEndMinutes = picked) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("結束 ${formatMinutesOfDay(state.windowEndMinutes)}")
                }
            }
            Text(
                text = "結束時間早於開始時間時，代表跨越午夜。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.intervalMinutes,
                onValueChange = { value ->
                    viewModel.updateForm { it.copy(intervalMinutes = value.filter(Char::isDigit)) }
                },
                label = { Text("檢查頻率（分鐘，至少 15）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            val error = state.error
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { viewModel.save() },
                enabled = !state.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (state.isNew) "新增網站" else "儲存變更")
                }
            }

            if (!state.isNew) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("刪除網站", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("刪除網站？") },
            text = { Text("「${state.name}」的設定與所有檢查紀錄都會一併刪除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) {
                    Text("刪除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }

    val duplicateWarning = state.duplicateWarning
    if (duplicateWarning != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateWarning,
            title = { Text("網址重複") },
            text = { Text("「$duplicateWarning」已使用相同網址，仍要儲存嗎？") },
            confirmButton = {
                TextButton(onClick = { viewModel.save(ignoreDuplicate = true) }) {
                    Text("仍要儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicateWarning) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun showTimePicker(context: Context, initialMinutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        true,
    ).show()
}
