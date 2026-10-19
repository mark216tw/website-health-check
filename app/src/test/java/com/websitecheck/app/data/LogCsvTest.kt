package com.websitecheck.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

class LogCsvTest {

    @Test
    fun `builds header and escapes fields`() {
        val log = LogWithSite(
            id = 1,
            siteId = 1,
            checkedAt = 0,
            success = true,
            httpStatus = 200,
            latencyMs = 123,
            errorType = null,
            errorMessage = null,
            effectiveUrl = "https://example.com/final",
            siteName = "A,B",
            siteUrl = "https://example.com",
        )

        val csv = LogCsv.build(listOf(log))

        assertTrue(csv.startsWith("\uFEFF網站名稱,網址,檢查時間,結果"))
        assertTrue(csv.contains("\"A,B\""))
        assertTrue(csv.contains(",成功,200,123,"))
        assertTrue(csv.contains("https://example.com/final"))
    }

    @Test
    fun `escapes double quotes and newlines`() {
        val log = LogWithSite(
            id = 1,
            siteId = 1,
            checkedAt = 0,
            success = false,
            httpStatus = 500,
            latencyMs = 10,
            errorType = "STATUS",
            errorMessage = "line1\nline2 \"quoted\"",
            effectiveUrl = null,
            siteName = "Test",
            siteUrl = "https://example.com",
        )

        val csv = LogCsv.build(listOf(log))

        assertTrue(csv.contains("\"line1\nline2 \"\"quoted\"\"\""))
        assertTrue(csv.contains(",失敗,500,10,STATUS,"))
    }
}
