package com.websitecheck.app.network

data class CheckOutcome(
    val success: Boolean,
    val httpStatus: Int?,
    val latencyMs: Long,
    val errorType: String?,
    val errorMessage: String?,
    val effectiveUrl: String?,
)
