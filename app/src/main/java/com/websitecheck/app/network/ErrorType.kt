package com.websitecheck.app.network

fun errorTypeLabel(type: String?): String = when (type) {
    null -> "正常"
    "DNS" -> "DNS 解析失敗"
    "TIMEOUT" -> "連線逾時"
    "TLS" -> "TLS／憑證錯誤"
    "NETWORK" -> "網路連線失敗"
    "STATUS" -> "HTTP 狀態碼異常"
    "KEYWORD" -> "內容未包含指定字串"
    "INVALID_URL" -> "網址格式錯誤"
    "OTHER" -> "其他錯誤"
    else -> type
}
