package com.websitecheck.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SiteSortOptionTest {

    @Test
    fun `name ascending ignores case`() {
        val sites = listOf(
            site(1, "beta"),
            site(2, "Alpha"),
            site(3, "gamma"),
        )

        assertEquals(listOf(2L, 1L, 3L), sortedIds(SiteSortOption.NAME_ASC, sites))
    }

    @Test
    fun `name descending ignores case`() {
        val sites = listOf(
            site(1, "beta"),
            site(2, "Alpha"),
            site(3, "gamma"),
        )

        assertEquals(listOf(3L, 1L, 2L), sortedIds(SiteSortOption.NAME_DESC, sites))
    }

    @Test
    fun `created descending puts newest first`() {
        val sites = listOf(
            site(1, "One", createdAt = 100),
            site(2, "Two", createdAt = 200),
            site(3, "Three", createdAt = 300),
        )

        assertEquals(listOf(3L, 2L, 1L), sortedIds(SiteSortOption.CREATED_DESC, sites))
    }

    @Test
    fun `status sorting puts failures first and disabled last`() {
        val failing = site(1, "Failing", lastSuccess = false)
        val never = site(2, "Never", lastSuccess = null)
        val ok = site(3, "Ok", lastSuccess = true)
        val disabled = site(4, "Disabled", lastSuccess = false, enabled = false)

        val sorted = listOf(ok, disabled, failing, never)
            .sortedWith(SiteSortOption.STATUS_FAILURE_FIRST.comparator())

        assertEquals(listOf(1L, 2L, 3L, 4L), sorted.map { it.id })
    }

    @Test
    fun `latency descending puts slowest first and unknown last`() {
        val sites = listOf(
            site(1, "Slow", lastLatencyMs = 800),
            site(2, "Unknown", lastLatencyMs = null),
            site(3, "Fast", lastLatencyMs = 200),
        )

        assertEquals(listOf(1L, 3L, 2L), sortedIds(SiteSortOption.LATENCY_DESC, sites))
    }

    @Test
    fun `last check descending puts null last`() {
        val sites = listOf(
            site(1, "Older", lastCheckAt = 100),
            site(2, "Never", lastCheckAt = null),
            site(3, "Newer", lastCheckAt = 300),
        )

        assertEquals(listOf(3L, 1L, 2L), sortedIds(SiteSortOption.LAST_CHECK_DESC, sites))
    }

    private fun sortedIds(option: SiteSortOption, sites: List<SiteEntity>): List<Long> =
        sites.sortedWith(option.comparator()).map { it.id }

    private fun site(
        id: Long,
        name: String,
        enabled: Boolean = true,
        createdAt: Long = id,
        lastCheckAt: Long? = null,
        lastSuccess: Boolean? = null,
        lastLatencyMs: Long? = null,
    ) = SiteEntity(
        id = id,
        name = name,
        url = "https://example.com",
        enabled = enabled,
        createdAt = createdAt,
        lastCheckAt = lastCheckAt,
        lastSuccess = lastSuccess,
        lastLatencyMs = lastLatencyMs,
    )
}
