package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_log_table")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val planetId: String?,
    val planetType: String?,
    val planetDisplayName: String?,
    val description: String,
    val valueBefore: Int?,
    val valueAfter: Int?,
    val coinDelta: Long?,
    val occurredAt: Long
)
