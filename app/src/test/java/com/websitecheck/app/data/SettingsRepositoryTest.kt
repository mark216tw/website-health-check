package com.websitecheck.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun `log cleanup is due initially and after one day`() {
        val dayMillis = 24L * 60L * 60L * 1_000L

        assertTrue(logCleanupIsDue(nowMillis = 1L, lastCleanupAt = 0L))
        assertTrue(logCleanupIsDue(nowMillis = dayMillis + 1L, lastCleanupAt = 1L))
    }

    @Test
    fun `log cleanup is not repeated within one day`() {
        val lastCleanupAt = 1_000L

        assertFalse(
            logCleanupIsDue(
                nowMillis = lastCleanupAt + 60_000L,
                lastCleanupAt = lastCleanupAt,
            ),
        )
    }

    @Test
    fun `log cleanup retries after clock moves backwards`() {
        assertTrue(logCleanupIsDue(nowMillis = 1_000L, lastCleanupAt = 2_000L))
    }
}
