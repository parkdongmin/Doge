package com.doge.simulator.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planet_table")
data class PlanetEntity(
    @PrimaryKey val id: String,

    // enum -> string
    val type: String,

    // 행성 속성 (고정 스탯)
    val production: Int,
    val risk: Int,
    val investment: Int,
    val eventRate: Int,

    // 구매 시 값
    val buyPrice: Int,
    val acquireTime: Long,

    // 변동 정보
    val currentValue: Int,
    val level: Int,
    val totalProfit: Long,

    // Idle 계산용
    val lastProfitTime: Long
)