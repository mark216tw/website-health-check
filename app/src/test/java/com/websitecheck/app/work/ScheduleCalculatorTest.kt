package com.websitecheck.app.work

import com.websitecheck.app.data.SiteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleCalculatorTest {
    private val zone = ZoneId.of("Asia/Taipei")

    @Test
    fun `first check runs immediately inside window`() {
        val site = site(start = 9 * 60, end = 17 * 60, interval = 60)
        val now = millisAt(2024, 1, 1, 10, 30)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(now, next)
    }

    @Test
    fun `first check starts at window start before window`() {
        val site = site(start = 9 * 60, end = 17 * 60, interval = 45)
        val now = millisAt(2024, 1, 1, 8, 15)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(millisAt(2024, 1, 1, 9, 0), next)
    }

    @Test
    fun `first check skips to next enabled day after window`() {
        val site = site(start = 9 * 60, end = 17 * 60, interval = 60)
        val now = millisAt(2024, 1, 1, 17, 30)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(millisAt(2024, 1, 8, 9, 0), next)
    }

    @Test
    fun `next check follows interval from last check`() {
        val site = site(
            start = 9 * 60,
            end = 17 * 60,
            interval = 60,
            lastCheckAt = millisAt(2024, 1, 1, 10, 30),
        )
        val now = millisAt(2024, 1, 1, 10, 35)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(millisAt(2024, 1, 1, 11, 30), next)
    }

    @Test
    fun `overdue check runs immediately`() {
        val site = site(
            start = 9 * 60,
            end = 17 * 60,
            interval = 60,
            lastCheckAt = millisAt(2024, 1, 1, 10, 30),
        )
        val now = millisAt(2024, 1, 1, 12, 0)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(now, next)
    }

    @Test
    fun `forced immediate check runs now inside window`() {
        val site = site(
            start = 9 * 60,
            end = 17 * 60,
            interval = 60,
            lastCheckAt = millisAt(2024, 1, 1, 10, 30),
        )
        val now = millisAt(2024, 1, 1, 10, 35)

        val next = ScheduleCalculator.nextRunMillis(
            site = site,
            nowMillis = now,
            zone = zone,
            forceImmediate = true,
        )

        assertEquals(now, next)
    }

    @Test
    fun `next check beyond window uses next window start`() {
        val site = site(
            start = 9 * 60,
            end = 17 * 60,
            interval = 60,
            lastCheckAt = millisAt(2024, 1, 1, 16, 50),
        )
        val now = millisAt(2024, 1, 1, 17, 0)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(millisAt(2024, 1, 8, 9, 0), next)
    }

    @Test
    fun `cross midnight first check runs immediately`() {
        val site = site(
            days = SUNDAY,
            start = 23 * 60,
            end = 60,
            interval = 30,
        )
        val now = millisAt(2023, 12, 31, 23, 45)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(now, next)
    }

    @Test
    fun `cross midnight next check follows interval`() {
        val site = site(
            days = SUNDAY,
            start = 23 * 60,
            end = 60,
            interval = 30,
            lastCheckAt = millisAt(2023, 12, 31, 23, 40),
        )
        val now = millisAt(2023, 12, 31, 23, 45)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(millisAt(2024, 1, 1, 0, 10), next)
    }

    @Test
    fun `clamps interval below fifteen minutes`() {
        val site = site(
            start = 9 * 60,
            end = 17 * 60,
            interval = 1,
            lastCheckAt = millisAt(2024, 1, 1, 9, 0),
        )
        val now = millisAt(2024, 1, 1, 9, 5)

        val next = ScheduleCalculator.nextRunMillis(site, now, zone)

        assertEquals(millisAt(2024, 1, 1, 9, 15), next)
    }

    @Test
    fun `returns null when no days selected`() {
        val site = site(days = 0, start = 9 * 60, end = 17 * 60, interval = 60)
        val now = millisAt(2024, 1, 1, 10, 0)

        assertNull(ScheduleCalculator.nextRunMillis(site, now, zone))
    }

    @Test
    fun `initial delay converts absolute run time to relative milliseconds`() {
        val now = millisAt(2024, 1, 1, 10, 0)
        val nextRun = millisAt(2024, 1, 1, 10, 15)

        assertEquals(15 * 60_000L, ScheduleCalculator.initialDelayMillis(nextRun, now))
    }

    @Test
    fun `initial delay is zero for immediate or overdue run`() {
        val now = millisAt(2024, 1, 1, 10, 0)

        assertEquals(0L, ScheduleCalculator.initialDelayMillis(now, now))
        assertEquals(0L, ScheduleCalculator.initialDelayMillis(now - 1L, now))
    }

    private fun site(
        days: Int = MONDAY,
        start: Int,
        end: Int,
        interval: Int,
        lastCheckAt: Long? = null,
    ) = SiteEntity(
        name = "Test",
        url = "https://example.com",
        daysOfWeek = days,
        windowStartMinutes = start,
        windowEndMinutes = end,
        intervalMinutes = interval,
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
    }
}
