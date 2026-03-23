package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planet_table")
data class PlanetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val buyPrice: Int,
    val acquireTime: Long,
    val currentValue: Int,
    val level: Int,
    val totalProfit: Long,
    val lastProfitTime: Long
)