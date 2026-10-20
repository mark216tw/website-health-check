package com.websitecheck.app.work

import com.websitecheck.app.data.SiteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class OverdueEvaluatorTest {
    private val zone = ZoneId.of("Asia/Taipei")

    @Test
    fun `disabled site is never overdue`() {
        val site = site(
            enabled = false,
            interval = 30,
            createdAt = millisAt(2024, 1, 1, 10, 0),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 12, 0), zone))
        assertNull(OverdueEvaluator.dueAtMillis(site, zone))
    }

    @Test
    fun `never checked site is overdue after grace period`() {
        val site = site(
            interval = 30,
            createdAt = millisAt(2024, 1, 1, 10, 0),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 10, 4), zone))
        assertTrue(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 10, 5), zone))
    }

    @Test
    fun `site inside interval is not overdue`() {
        val site = site(
            interval = 30,
            lastCheckAt = millisAt(2024, 1, 1, 10, 0),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 10, 29), zone))
    }

    @Test
    fun `site becomes overdue after interval plus grace`() {
        val site = site(
            interval = 30,
            lastCheckAt = millisAt(2024, 1, 1, 10, 0),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 10, 34), zone))
        assertTrue(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 10, 35), zone))
        assertEquals(
            5 * 60_000L,
            OverdueEvaluator.overdueForMillis(site, millisAt(2024, 1, 1, 10, 35), zone),
        )
    }

    @Test
    fun `site is not overdue outside its window`() {
        val site = site(
            interval = 30,
            start = 9 * 60,
            end = 17 * 60,
            lastCheckAt = millisAt(2024, 1, 1, 16, 50),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 18, 0), zone))
    }

    @Test
    fun `site is overdue at next window start`() {
        val site = site(
            days = ALL_DAYS,
            interval = 30,
            start = 9 * 60,
            end = 17 * 60,
            lastCheckAt = millisAt(2024, 1, 1, 16, 50),
        )

        assertTrue(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 2, 9, 5), zone))
    }

    @Test
    fun `site is not overdue on a disabled weekday`() {
        val site = site(
            days = MONDAY,
            interval = 30,
            lastCheckAt = millisAt(2024, 1, 1, 10, 0),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 3, 12, 0), zone))
    }

    @Test
    fun `cross midnight site becomes overdue in the morning`() {
        val site = site(
            days = SUNDAY,
            interval = 30,
            start = 23 * 60,
            end = 60,
            lastCheckAt = millisAt(2023, 12, 31, 23, 40),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 0, 12), zone))
        assertTrue(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 0, 15), zone))
    }

    @Test
    fun `site with no configured days is never overdue`() {
        val site = site(
            days = 0,
            interval = 30,
            lastCheckAt = millisAt(2024, 1, 1, 10, 0),
        )

        assertFalse(OverdueEvaluator.isOverdue(site, millisAt(2024, 1, 1, 12, 0), zone))
    }

    private fun site(
        enabled: Boolean = true,
        days: Int = MONDAY,
        start: Int = 0,
        end: Int = 23 * 60 + 59,
        interval: Int,
        createdAt: Long = millisAt(2024, 1, 1, 0, 0),
        lastCheckAt: Long? = null,
    ) = SiteEntity(
        name = "Test",
        url = "https://example.com",
        enabled = enabled,
        daysOfWeek = days,
        windowStartMinutes = start,
        windowEndMinutes = end,
        intervalMinutes = interval,
        createdAt = createdAt,
        lastCheckAt = lastCheckAt,
    )

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    companion object {
        private const val MONDAY = 1
        private const val SUNDAY = 1 shl 6
        private const val ALL_DAYS = 0x7F
    }
}
