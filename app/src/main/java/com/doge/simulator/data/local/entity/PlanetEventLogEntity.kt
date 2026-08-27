package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planet_event_log_table")
data class PlanetEventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planetId: String,
    val planetDisplayName: String,
    val planetVariantCode: String,
    val isPositive: Boolean,
    val flavorText: String,
    val productionDeltaPerHour: Long,
    val marketDelta: Long,
    val occurredAt: Long
)
