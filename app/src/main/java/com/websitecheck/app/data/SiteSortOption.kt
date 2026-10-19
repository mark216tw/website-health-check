package com.websitecheck.app.data

enum class SiteSortOption(val label: String) {
    CREATED_ASC("新增時間（舊→新）"),
    CREATED_DESC("新增時間（新→舊）"),
    NAME_ASC("名稱（A→Z）"),
    NAME_DESC("名稱（Z→A）"),
    LAST_CHECK_DESC("最近檢查（新→舊）"),
    LAST_CHECK_ASC("最近檢查（舊→新）"),
    STATUS_FAILURE_FIRST("狀態（異常優先）"),
    LATENCY_DESC("回應時間（慢→快）"),
    LATENCY_ASC("回應時間（快→慢）");

    fun comparator(): Comparator<SiteEntity> = when (this) {
        CREATED_ASC -> compareBy<SiteEntity> { it.createdAt }.thenBy { it.id }
        CREATED_DESC -> compareByDescending<SiteEntity> { it.createdAt }.thenByDescending { it.id }
        NAME_ASC -> compareBy<SiteEntity> { it.name.lowercase() }
            .thenBy { it.createdAt }
        NAME_DESC -> compareByDescending<SiteEntity> { it.name.lowercase() }
            .thenBy { it.createdAt }
        LAST_CHECK_DESC -> compareByDescending<SiteEntity> { it.lastCheckAt ?: Long.MIN_VALUE }
            .thenBy { it.createdAt }
        LAST_CHECK_ASC -> compareBy<SiteEntity> { it.lastCheckAt ?: Long.MAX_VALUE }
            .thenBy { it.createdAt }
        STATUS_FAILURE_FIRST -> compareBy<SiteEntity> { statusRank(it) }
            .thenBy { it.name.lowercase() }
        LATENCY_DESC -> compareByDescending<SiteEntity> { it.lastLatencyMs ?: Long.MIN_VALUE }
            .thenBy { it.createdAt }
        LATENCY_ASC -> compareBy<SiteEntity> { it.lastLatencyMs ?: Long.MAX_VALUE }
            .thenBy { it.createdAt }
    }

    companion object {
        private fun statusRank(site: SiteEntity): Int = when {
            !site.enabled -> 3
            site.lastSuccess == false -> 0
            site.lastSuccess == null -> 1
            else -> 2
        }
    }
}
