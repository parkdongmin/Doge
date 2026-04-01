package com.doge.simulator.domain.model

import java.util.UUID

data class Planet(
    val id: String = UUID.randomUUID().toString(),
    val type: PlanetType,

    // 스탯 (행성 자체 속성)
    val production: Int,
    val risk: Int,
    val investment: Int,
    val eventRate: Int,

    // 구매 시 결정되는 값
    val buyPrice: Int,
    val acquireTime: Long = System.currentTimeMillis(),

    // 변동 정보
    val currentValue: Int,
    val level: Int = 1,
    val totalProfit: Long = 0L,

    // Idle 계산용
    val lastProfitTime: Long = System.currentTimeMillis()
)