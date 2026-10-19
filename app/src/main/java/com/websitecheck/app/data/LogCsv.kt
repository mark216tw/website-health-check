package com.websitecheck.app.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object LogCsv {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun build(logs: List<LogWithSite>): String {
        val builder = StringBuilder("\uFEFF")
        builder.append("網站名稱,網址,檢查時間,結果,HTTP狀態,回應時間(ms),錯誤類型,錯誤訊息,最終網址\r\n")
        logs.forEach { log ->
            val time = Instant.ofEpochMilli(log.checkedAt)
                .atZone(ZoneId.systemDefault())
                .format(timeFormatter)
            builder.append(field(log.siteName)).append(',')
                .append(field(log.siteUrl)).append(',')
                .append(time).append(',')
                .append(if (log.success) "成功" else "失敗").append(',')
                .append(log.httpStatus?.toString().orEmpty()).append(',')
                .append(log.latencyMs).append(',')
                .append(field(log.errorType)).append(',')
                .append(field(log.errorMessage)).append(',')
                .append(field(log.effectiveUrl)).append("\r\n")
        }
        return builder.toString()
    }

    private fun field(value: String?): String {
        val text = value.orEmpty()
        return if (text.contains(',') || text.contains('"') || text.contains('\n') || text.contains('\r')) {
            "\"" + text.replace("\"", "\"\"") + "\""
        } else {
            text
        }
    }
}
