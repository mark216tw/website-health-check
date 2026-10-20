package com.websitecheck.app.ui

import com.websitecheck.app.data.SiteEntity
import com.websitecheck.app.work.ScheduleCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
private val fullTimeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss")
private val days = listOf("一", "二", "三", "四", "五", "六", "日")

fun formatTimestamp(millis: Long?): String {
    if (millis == null) return "尚未檢查"
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
}

fun formatFullTimestamp(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fullTimeFormatter)

fun formatMinutesOfDay(minutes: Int): String {
    val normalized = minutes.coerceIn(0, 24 * 60)
    return "%02d:%02d".format(normalized / 60, normalized % 60)
}

fun formatLatency(millis: Long?): String = if (millis == null) "-" else "${millis} ms"

fun formatAge(millis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
    if (millis == null) return "尚未檢查"
    val minutes = ((nowMillis - millis).coerceAtLeast(0L)) / 60_000L
    return when {
        minutes < 1 -> "剛剛"
        minutes < 60 -> "$minutes 分鐘前"
        minutes < 24 * 60 -> "${minutes / 60} 小時前"
        else -> "${minutes / (24 * 60)} 天前"
    }
}

fun formatInterval(minutes: Int): String =
    if (minutes % 60 == 0) "每 ${minutes / 60} 小時" else "每 $minutes 分鐘"

fun formatDays(daysOfWeek: Int): String {
    val selected = days.filterIndexed { index, _ -> daysOfWeek and (1 shl index) != 0 }
    return when {
        selected.isEmpty() -> "無"
        selected.size == 7 -> "每日"
        else -> selected.joinToString("、")
    }
}

fun formatSchedule(site: SiteEntity): String {
    val window = "${formatMinutesOfDay(site.windowStartMinutes)}–${formatMinutesOfDay(site.windowEndMinutes)}"
    return "${formatDays(site.daysOfWeek)} ${window}，${formatInterval(site.intervalMinutes)}"
}

fun formatNextRun(site: SiteEntity, nowMillis: Long = System.currentTimeMillis()): String {
    if (!site.enabled) return "未啟用"
    val next = ScheduleCalculator.nextRunMillis(site, nowMillis) ?: return "無排程"
    return formatTimestamp(next)
}
