package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planet_table")
data class PlanetEntity(
    @PrimaryKey val id: String,
    val type: String,
    val production: Int,
    val risk: Int,
    val investment: Int,
    val eventRate: Int,
    val buyPrice: Int,
    val acquireTime: Long,
    val currentValue: Int,
    val level: Int,
    val totalProfit: Long,
    val variantId: String = "",
    val upgradeInvestment: Long = 0L,
    val lastProfitTime: Long
)