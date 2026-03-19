package com.doge.simulator.domain.model

data class PlanetMetaData(
    val type: PlanetType,
    val displayName: String,
    val description: String,
    val baseProduction: Int,      // 기본 자원 생산량
    val baseRisk: Int,            // 위험도 (0~100)
    val baseInvestmentCost: Int,  // 투자 기본 비용
    val eventRate: Int            // 랜덤 이벤트 확률 (0~100)
)