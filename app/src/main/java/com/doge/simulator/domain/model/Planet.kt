package com.doge.simulator.domain.model

import java.util.UUID

data class Planet(
    val id: String = UUID.randomUUID().toString(),
    val type: PlanetType,

    val production: Int,
    val risk: Int,
    val investment: Int,
    val eventRate: Int,

    val buyPrice: Int,
    val acquireTime: Long = System.currentTimeMillis(),

    val currentValue: Int,
    val level: Int = 1,
    val totalProfit: Long = 0L,

    val variantId: String = "",

    // 강화에 투자한 누적 코인 (매도 가격 산정에 사용)
    val upgradeInvestment: Long = 0L,

    val lastProfitTime: Long = System.currentTimeMillis()
)

// 강화 레벨이 오를수록 실제 생산량도 함께 오르도록 보정한 값
val Planet.effectiveProduction: Long
    get() = (production * GameConstants.PLANET_PRODUCTION_SCALE * GameConstants.planetLevelMultiplier(level)).toLong()