package com.websitecheck.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val method: String = "HEAD",
    val timeoutMs: Int = 10_000,
    val expectedStatusMin: Int = 200,
    val expectedStatusMax: Int = 399,
    val followRedirects: Boolean = true,
    val keywordCheckEnabled: Boolean = false,
    val expectedKeyword: String = "",
    val failureThreshold: Int = 3,
    val daysOfWeek: Int = 0x7F,
    val windowStartMinutes: Int = 0,
    val windowEndMinutes: Int = 23 * 60 + 59,
    val intervalMinutes: Int = 60,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCheckAt: Long? = null,
    val lastSuccess: Boolean? = null,
    val lastLatencyMs: Long? = null,
    val lastStatus: Int? = null,
    val lastError: String? = null,
    val consecutiveFailures: Int = 0,
    val notifiedFailure: Boolean = false,
)
