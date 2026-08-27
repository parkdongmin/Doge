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

    val lastProfitTime: Long = System.currentTimeMillis(),

    // 행성 시세/이벤트 시스템 — 이벤트가 터질 때마다 덮어써짐(누적 곱 아님), 매도로만 정리 가능
    val productionMultiplier: Double = 1.0,
    val marketAdjustment: Long = 0L,
    val lastEventTime: Long = System.currentTimeMillis()
)

// 강화 레벨이 오를수록 실제 생산량도 함께 오르도록 보정한 값 (분당 정수 표시용 — UI에 그대로 노출).
// productionMultiplier(이벤트로 흔들리는 배율)도 곱해져 온라인/오프라인 상관없이 항상 실제
// 수령액과 일치 — PLANET_PRODUCTION_SCALE·레벨 배율 같은 튜닝된 공식 자체는 안 건드림
val Planet.effectiveProduction: Long
    get() = (production * GameConstants.PLANET_PRODUCTION_SCALE * GameConstants.planetLevelMultiplier(level) * productionMultiplier).toLong()

// 실제 수익 정산(경과 시간 × 생산량)에 쓰는 내림하지 않은 값. effectiveProduction은 표시용으로
// 미리 내림된 값이라, 이걸 경과 분(최대 1440분)에 곱해 정산하면 소수점 손실이 누적돼 플레이어가
// 최대 수 % 손해를 본다 — 정산은 항상 이 값을 쓰고, 최종 합계에서만 한 번 내림한다
val Planet.preciseProduction: Double
    get() = production * GameConstants.PLANET_PRODUCTION_SCALE * GameConstants.planetLevelMultiplier(level) * productionMultiplier

// 매도가·랭킹에 쓰이는 행성 시장가치. marketAdjustment(이벤트로 흔들리는 시세)가 반영되므로
// 항상 이 값을 통해서만 가치를 계산한다 (buyPrice + upgradeInvestment를 직접 더하지 말 것)
val Planet.marketValue: Long
    get() = buyPrice + upgradeInvestment + marketAdjustment