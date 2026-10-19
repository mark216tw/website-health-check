package com.websitecheck.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_logs",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("siteId"), Index("checkedAt")],
)
data class CheckLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val siteId: Long,
    val checkedAt: Long,
    val success: Boolean,
    val httpStatus: Int?,
    val latencyMs: Long,
    val errorType: String?,
    val errorMessage: String?,
    val effectiveUrl: String?,
)
