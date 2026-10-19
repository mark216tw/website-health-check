package com.websitecheck.app.network

import com.websitecheck.app.data.SiteEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class HealthChecker {
    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun check(site: SiteEntity): CheckOutcome = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        val timeout = site.timeoutMs.coerceIn(1_000, 60_000).toLong()
        val client = baseClient.newBuilder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .callTimeout(timeout + 2_000, TimeUnit.MILLISECONDS)
            .followRedirects(site.followRedirects)
            .followSslRedirects(site.followRedirects)
            .build()

        try {
            val url = normalizeUrl(site.url)
            val useHead = site.method.equals("HEAD", ignoreCase = true) && !site.keywordCheckEnabled
            var request = requestBuilder(url, useHead)
            var response = client.newCall(request).execute()
            if (useHead && response.code in HEAD_FALLBACK_CODES) {
                response.close()
                request = requestBuilder(url, useHead = false)
                response = client.newCall(request).execute()
            }
            response.use { result ->
                val latencyMs = elapsedMs(startedAt)
                val statusOk = result.code in site.expectedStatusMin..site.expectedStatusMax
                val effectiveUrl = result.request.url.toString()
                when {
                    !statusOk -> CheckOutcome(
                        success = false,
                        httpStatus = result.code,
                        latencyMs = latencyMs,
                        errorType = "STATUS",
                        errorMessage = "HTTP ${result.code}",
                        effectiveUrl = effectiveUrl,
                    )

                    site.keywordCheckEnabled -> {
                        val content = result.peekBody(MAX_CONTENT_BYTES).string()
                        if (content.contains(site.expectedKeyword)) {
                            CheckOutcome(
                                success = true,
                                httpStatus = result.code,
                                latencyMs = latencyMs,
                                errorType = null,
                                errorMessage = null,
                                effectiveUrl = effectiveUrl,
                            )
                        } else {
                            CheckOutcome(
                                success = false,
                                httpStatus = result.code,
                                latencyMs = latencyMs,
                                errorType = "KEYWORD",
                                errorMessage = "內容未包含「${site.expectedKeyword}」",
                                effectiveUrl = effectiveUrl,
                            )
                        }
                    }

                    else -> CheckOutcome(
                        success = true,
                        httpStatus = result.code,
                        latencyMs = latencyMs,
                        errorType = null,
                        errorMessage = null,
                        effectiveUrl = effectiveUrl,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CheckOutcome(
                success = false,
                httpStatus = null,
                latencyMs = elapsedMs(startedAt),
                errorType = classify(e),
                errorMessage = e.message ?: e.javaClass.simpleName,
                effectiveUrl = null,
            )
        }
    }

    private fun requestBuilder(url: String, useHead: Boolean): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", "WebsiteHealthCheck/1.0 (Android)")
        return if (useHead) builder.head().build() else builder.get().build()
    }

    private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

    private fun classify(error: Exception): String = when (error) {
        is UnknownHostException -> "DNS"
        is SocketTimeoutException -> "TIMEOUT"
        is SSLException -> "TLS"
        is IllegalArgumentException -> "INVALID_URL"
        is IOException -> "NETWORK"
        else -> "OTHER"
    }

    companion object {
        private val HEAD_FALLBACK_CODES = setOf(405, 501)
        private const val MAX_CONTENT_BYTES = 1_000_000L

        fun normalizeUrl(raw: String): String {
            val trimmed = raw.trim()
            val lower = trimmed.lowercase()
            return if (lower.startsWith("http://") || lower.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
        }

        fun canonicalUrl(raw: String): String {
            val normalized = normalizeUrl(raw)
            return normalized.toHttpUrlOrNull()?.toString() ?: normalized
        }

        fun isValidUrl(raw: String): Boolean {
            val normalized = normalizeUrl(raw)
            if (normalized.isBlank()) return false
            return try {
                normalized.toHttpUrlOrNull() != null
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}
