package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expedition_report_table")
data class ExpeditionReportEntity(
    @PrimaryKey val expeditionId: String,
    val recordNumber: Int,
    val chapter: Int,
    val chapterTitle: String,
    val recordTitle: String,
    val isChapterEnding: Boolean,
    val isRead: Boolean,
    val completedAt: Long
)
