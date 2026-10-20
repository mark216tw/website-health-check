package com.websitecheck.app.work

import com.websitecheck.app.data.SiteEntity
import java.time.ZoneId

object OverdueEvaluator {
    const val DEFAULT_GRACE_MILLIS = 5 * 60_000L

    fun isOverdue(
        site: SiteEntity,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        graceMillis: Long = DEFAULT_GRACE_MILLIS,
    ): Boolean {
        if (!site.enabled) return false
        val currentRun = ScheduleCalculator.nextRunMillis(site, nowMillis, zone) ?: return false
        if (currentRun > nowMillis) return false
        val dueAt = dueAtMillis(site, zone) ?: return false
        return nowMillis >= dueAt + graceMillis
    }

    fun dueAtMillis(
        site: SiteEntity,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (!site.enabled) return null
        val anchorMillis = site.lastCheckAt ?: site.createdAt
        return ScheduleCalculator.nextRunMillis(
            site = site,
            nowMillis = anchorMillis,
            zone = zone,
            forceImmediate = false,
        )
    }

    fun overdueForMillis(
        site: SiteEntity,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        graceMillis: Long = DEFAULT_GRACE_MILLIS,
    ): Long {
        if (!isOverdue(site, nowMillis, zone, graceMillis)) return 0L
        val dueAt = dueAtMillis(site, zone) ?: return 0L
        return (nowMillis - dueAt).coerceAtLeast(0L)
    }
}
