package com.websitecheck.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.websitecheck.app.MainActivity
import com.websitecheck.app.R
import com.websitecheck.app.data.AppSettings
import com.websitecheck.app.data.SiteEntity
import com.websitecheck.app.data.SettingsRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationHelper(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    init {
        createChannel()
    }

    fun notifyFailure(site: SiteEntity): Boolean {
        if (!canNotify()) return false
        val detail = site.lastError ?: site.lastStatus?.let { "HTTP $it" } ?: "檢查失敗"
        val text = buildString {
            append(detail)
            if (site.lastLatencyMs != null) append("，回應時間 ${site.lastLatencyMs} ms")
            append("，連續失敗 ${site.consecutiveFailures} 次")
            append("（").append(formatTime(site.lastCheckAt)).append("）")
        }
        return post(
            id = notificationId(site.id),
            title = "網站異常：${site.name}",
            text = text,
        )
    }

    fun notifyRecovery(site: SiteEntity): Boolean {
        if (!canNotify()) return false
        val text = buildString {
            append("已恢復正常")
            if (site.lastStatus != null) append("，HTTP ${site.lastStatus}")
            if (site.lastLatencyMs != null) append("，回應時間 ${site.lastLatencyMs} ms")
            append("（").append(formatTime(site.lastCheckAt)).append("）")
        }
        return post(
            id = notificationId(site.id),
            title = "網站恢復正常：${site.name}",
            text = text,
        )
    }

    private fun canNotify(): Boolean {
        val settings: AppSettings = settingsRepository.settings.value
        if (!settings.notificationsEnabled) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL_ID)
            channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun post(id: Int, title: String, text: String): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (!canNotify()) return false
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (e: SecurityException) {
            // Permission can be revoked between the check and the call.
            false
        }
    }

    private fun createChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "網站狀態通知",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "網站檢查失敗與恢復通知"
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(siteId: Long): Int = (siteId * 2).toInt()

    private fun formatTime(millis: Long?): String {
        if (millis == null) return "未知時間"
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
    }

    companion object {
        const val CHANNEL_ID = "website_status"
    }
}
