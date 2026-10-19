package com.websitecheck.app.work

import com.websitecheck.app.data.SiteEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object ScheduleCalculator {
    private const val MIN_INTERVAL_MINUTES = 15
    private const val DAY_BITS = 0x7F
    private const val MILLIS_PER_MINUTE = 60_000L

    fun nextRunMillis(
        site: SiteEntity,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        forceImmediate: Boolean = false,
    ): Long? {
        if (site.daysOfWeek and DAY_BITS == 0) return null

        val interval = site.intervalMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES).toLong()
        val intervalMillis = interval * MILLIS_PER_MINUTE
        val targetMillis = if (forceImmediate || site.lastCheckAt == null) {
            nowMillis
        } else {
            maxOf(nowMillis, site.lastCheckAt + intervalMillis)
        }

        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val today = now.toLocalDate()
        var best: ZonedDateTime? = null

        for (dayOffset in -1L..7L) {
            val day = today.plusDays(dayOffset)
            val dayBit = 1 shl (day.dayOfWeek.value - 1)
            if (site.daysOfWeek and dayBit == 0) continue

            val start = day.atStartOfDay(zone)
                .plusMinutes(site.windowStartMinutes.coerceIn(0, 24 * 60).toLong())
            var end = day.atStartOfDay(zone)
                .plusMinutes(site.windowEndMinutes.coerceIn(0, 24 * 60).toLong())
            if (!end.isAfter(start)) {
                end = start.plusDays(1)
            }

            val startMillis = start.toInstant().toEpochMilli()
            val endMillis = end.toInstant().toEpochMilli()
            val candidate = when {
                targetMillis < startMillis -> start
                targetMillis < endMillis -> Instant.ofEpochMilli(targetMillis).atZone(zone)
                else -> null
            }

            if (candidate != null && (best == null || candidate.isBefore(best))) {
                best = candidate
            }
        }

        return best?.toInstant()?.toEpochMilli()
    }

    internal fun initialDelayMillis(nextRunMillis: Long, nowMillis: Long): Long =
        (nextRunMillis - nowMillis).coerceAtLeast(0L)
}
