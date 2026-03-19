package com.doge.simulator.domain.model

import java.util.UUID

data class Planet(
    val id: String = UUID.randomUUID().toString(),
    val type: PlanetType,

    // 구매 시 결정되는 값
    val buyPrice: Int,
    val acquireTime: Long = System.currentTimeMillis(),

    // 변동 정보
    val currentValue: Int,
    val level: Int = 1,
    val totalProfit: Long = 0L,

    // Idle 계산용
    val lastProfitTime: Long = System.currentTimeMillis()
) {

    // 메타데이터 가져오기
    val meta: PlanetMetaData
        get() = PlanetMetaDataTable.data[type]!!

    // 기본 생산량 = baseProduction × 레벨 배율
    fun getProductionPerHour(): Int {
        return meta.baseProduction * level
    }

    // 현재 가치(시세) 산출
    fun calculateCurrentValue(): Int {
        return meta.baseInvestmentCost + (level * 200) + (currentValue)
    }

    // 위험도 기반 시세 변동
    fun applyRiskVariation(): Int {
        val risk = meta.baseRisk
        val random = (-risk..risk).random()
        return currentValue + random
    }
}