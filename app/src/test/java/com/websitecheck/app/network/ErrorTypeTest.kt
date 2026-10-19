package com.websitecheck.app.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTypeTest {

    @Test
    fun `maps known error types`() {
        assertEquals("DNS 解析失敗", errorTypeLabel("DNS"))
        assertEquals("連線逾時", errorTypeLabel("TIMEOUT"))
        assertEquals("TLS／憑證錯誤", errorTypeLabel("TLS"))
        assertEquals("網路連線失敗", errorTypeLabel("NETWORK"))
        assertEquals("HTTP 狀態碼異常", errorTypeLabel("STATUS"))
        assertEquals("內容未包含指定字串", errorTypeLabel("KEYWORD"))
        assertEquals("網址格式錯誤", errorTypeLabel("INVALID_URL"))
        assertEquals("其他錯誤", errorTypeLabel("OTHER"))
    }

    @Test
    fun `keeps unknown error type unchanged`() {
        assertEquals("UNKNOWN", errorTypeLabel("UNKNOWN"))
    }

    @Test
    fun `null type means normal`() {
        assertEquals("正常", errorTypeLabel(null))
    }
}
