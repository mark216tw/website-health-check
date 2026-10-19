package com.websitecheck.app.network

import com.websitecheck.app.data.SiteEntity
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HealthCheckerTest {
    private lateinit var server: MockWebServer
    private lateinit var checker: HealthChecker

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        checker = HealthChecker()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `successful response is recorded`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200))

        val outcome = checker.check(site(server.url("/ok").toString()))

        assertTrue(outcome.success)
        assertEquals(200, outcome.httpStatus)
        assertEquals(null, outcome.errorType)
        assertNotNull(outcome.effectiveUrl)
    }

    @Test
    fun `unexpected status code is a failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val outcome = checker.check(site(server.url("/error").toString()))

        assertFalse(outcome.success)
        assertEquals(500, outcome.httpStatus)
        assertEquals("STATUS", outcome.errorType)
    }

    @Test
    fun `follows redirect to final url`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "/final"),
        )
        server.enqueue(MockResponse().setResponseCode(200))

        val outcome = checker.check(site(server.url("/start").toString()))

        assertTrue(outcome.success)
        assertTrue(outcome.effectiveUrl.orEmpty().endsWith("/final"))
    }

    @Test
    fun `does not follow redirect when disabled`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "/final"),
        )

        val outcome = checker.check(
            site(server.url("/start").toString(), followRedirects = false),
        )

        assertTrue(outcome.success)
        assertEquals(302, outcome.httpStatus)
    }

    @Test
    fun `invalid url is classified`() = runBlocking {
        val outcome = checker.check(site("not a valid url"))

        assertFalse(outcome.success)
        assertEquals("INVALID_URL", outcome.errorType)
    }

    @Test
    fun `keyword check passes and forces GET`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<html><body>系統正常運作</body></html>"),
        )

        val outcome = checker.check(
            site(
                url = server.url("/page").toString(),
                method = "HEAD",
                keywordCheckEnabled = true,
                expectedKeyword = "系統正常運作",
            ),
        )

        assertTrue(outcome.success)
        assertEquals(200, outcome.httpStatus)
        assertEquals(null, outcome.errorType)
        assertEquals("GET", server.takeRequest().method)
    }

    @Test
    fun `keyword missing is a failure`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("<html><body>歡迎光臨</body></html>"),
        )

        val outcome = checker.check(
            site(
                url = server.url("/page").toString(),
                keywordCheckEnabled = true,
                expectedKeyword = "系統正常運作",
            ),
        )

        assertFalse(outcome.success)
        assertEquals(200, outcome.httpStatus)
        assertEquals("KEYWORD", outcome.errorType)
        assertTrue(outcome.errorMessage.orEmpty().contains("系統正常運作"))
    }

    @Test
    fun `keyword check is case sensitive`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("hello world"),
        )

        val outcome = checker.check(
            site(
                url = server.url("/page").toString(),
                keywordCheckEnabled = true,
                expectedKeyword = "Hello",
            ),
        )

        assertFalse(outcome.success)
        assertEquals("KEYWORD", outcome.errorType)
    }

    @Test
    fun `keyword check disabled ignores content`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("nothing here"),
        )

        val outcome = checker.check(
            site(
                url = server.url("/page").toString(),
                keywordCheckEnabled = false,
                expectedKeyword = "系統正常運作",
            ),
        )

        assertTrue(outcome.success)
        assertEquals(null, outcome.errorType)
    }

    @Test
    fun `canonical url normalizes host and path`() {
        assertEquals("https://example.com/", HealthChecker.canonicalUrl("example.com"))
        assertEquals("https://example.com/path", HealthChecker.canonicalUrl("HTTPS://EXAMPLE.COM/path"))
        assertEquals(
            HealthChecker.canonicalUrl("https://example.com/"),
            HealthChecker.canonicalUrl("https://example.com"),
        )
    }

    private fun site(
        url: String,
        followRedirects: Boolean = true,
        method: String = "HEAD",
        keywordCheckEnabled: Boolean = false,
        expectedKeyword: String = "",
    ) = SiteEntity(
        name = "Test",
        url = url,
        method = method,
        followRedirects = followRedirects,
        keywordCheckEnabled = keywordCheckEnabled,
        expectedKeyword = expectedKeyword,
        timeoutMs = 5_000,
    )
}
