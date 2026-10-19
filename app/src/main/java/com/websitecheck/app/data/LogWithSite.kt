package com.websitecheck.app.data

data class LogWithSite(
    val id: Long,
    val siteId: Long,
    val checkedAt: Long,
    val success: Boolean,
    val httpStatus: Int?,
    val latencyMs: Long,
    val errorType: String?,
    val errorMessage: String?,
    val effectiveUrl: String?,
    val siteName: String,
    val siteUrl: String,
)
