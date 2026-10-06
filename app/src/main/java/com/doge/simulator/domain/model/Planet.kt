package com.doge.simulator.domain.model

import java.util.UUID
import kotlin.math.roundToLong

data class Planet(
    val id: String = UUID.randomUUID().toString(),
    val type: PlanetType,

    val production: Int,
    val risk: Int,
    val investment: Int,
    val eventRate: Int,

    val buyPrice: Int,
    val acquireTime: Long = System.currentTimeMillis(),

    val level: Int = 1,
    val totalProfit: Long = 0L,

    val variantId: String = "",

    // 강화에 투자한 누적 코인 (매도 가격 산정에 사용)
    val upgradeInvestment: Long = 0L,

    val lastProfitTime: Long = System.currentTimeMillis(),

    // 행성 시세/이벤트 시스템 — 정상 상태의 생산 배율(0.5~2.0). 호재 +, 악재 − 로 누적되고,
    // 고장 중에는 그대로 멈춰 있다가 정비하면 이 배율로 다시 생산한다
    val productionMultiplier: Double = 1.0,
    // 생산 배율로부터 계산한 "정상 상태" 시세 보정. 고장 할인은 marketValue에서 따로 적용
    val marketAdjustment: Long = 0L,
    val lastEventTime: Long = System.currentTimeMillis(),

    // 이벤트로 생산량이 바뀌기 직전까지 쌓였지만 아직 수령 안 한 코인과, 그 적립이 어느 시점까지의
    // 몫인지(0 = 적립 없음). 앱을 안 연 사이 백그라운드에서 이벤트가 터져도 바뀌기 전 시간은 바뀌기
    // 전 생산량으로 쳐주기 위함 — 없으면 수령 시 전체 방치 시간이 새 생산량으로 소급 계산된다
    val bankedProfit: Double = 0.0,
    val bankedUntil: Long = 0L,

    // 고장(마이너스 생산) 상태의 손해 배율. 0 = 정상, 0.5~2.0 = 고장. 정비하면 0으로 돌아간다
    val lossMultiplier: Double = 0.0
)

val Planet.isBroken: Boolean
    get() = lossMultiplier > 0.0

// 강화 레벨이 오를수록 실제 생산량도 함께 오르도록 보정한 값 (분당 정수 표시용 — UI에 그대로 노출).
// productionMultiplier(이벤트로 흔들리는 배율)·고장 여부도 반영돼 온라인/오프라인 상관없이 항상
// 실제 수령액과 일치
val Planet.effectiveProduction: Long
    get() = preciseProduction.toLong()

// 실제 수익 정산(경과 시간 × 생산량)에 쓰는 내림하지 않은 값. effectiveProduction은 표시용으로
// 미리 내림된 값이라, 이걸 경과 분(최대 1440분)에 곱해 정산하면 소수점 손실이 누적돼 플레이어가
// 최대 수 % 손해를 본다 — 정산은 항상 이 값을 쓰고, 최종 합계에서만 한 번 내림한다
val Planet.preciseProduction: Double
    get() = if (isBroken) -normalProductionAtLevel(level) * lossMultiplier else normalProductionAtLevel(level)

// 고장이 아닐 때(정비 후)의 분당 생산량. 강화 미리보기·정비 비용·고장 손해의 기준값
fun Planet.normalProductionAtLevel(level: Int): Double =
    production * GameConstants.PLANET_PRODUCTION_SCALE * GameConstants.planetLevelMultiplier(level) * productionMultiplier

val Planet.normalProduction: Double
    get() = normalProductionAtLevel(level)

// 정비 비용 — 정상일 때 생산량의 2시간치
val Planet.maintenanceCost: Long
    get() = (normalProduction * GameConstants.PLANET_MAINTENANCE_COST_MINUTES).toLong().coerceAtLeast(1L)

// 마지막 수령 이후 정산 대상인 경과 분(정수). 오래 비워도 MAX_OFFLINE_MINUTES까지만 쳐준다.
fun Planet.elapsedProfitMinutes(now: Long): Long =
    minOf((now - lastProfitTime) / 60_000L, GameConstants.MAX_OFFLINE_MINUTES)

// lastProfitTime부터 [time]까지의 분(소수, 상한 적용). 상한은 앞쪽부터 채운다 — 처음 24시간만 생산.
private fun Planet.cappedMinutesUntil(time: Long): Double =
    ((time - lastProfitTime) / 60_000.0).coerceIn(0.0, GameConstants.MAX_OFFLINE_MINUTES.toDouble())

private val Planet.currentRateSince: Long
    get() = if (bankedUntil > 0L) bankedUntil else lastProfitTime

// 생산량이 바뀌기 직전([time])까지의 미수령 수익 — 이벤트·정비가 생산량을 바꾸기 전에 이 값을 적립해 둔다
fun Planet.bankedProfitAt(time: Long): Double =
    bankedProfit + preciseProduction * (cappedMinutesUntil(time) - cappedMinutesUntil(currentRateSince))

// 지금 수령할 미수령 수익 = 적립분 + 마지막 생산량 변화 이후 현재 생산량으로 번 몫.
// 경과 분은 기존처럼 정수로 내림한 값 기준(수령 시 lastProfitTime을 now로 당기므로)
fun Planet.pendingProfitAt(now: Long): Double {
    val currentMinutes = (elapsedProfitMinutes(now) - cappedMinutesUntil(currentRateSince)).coerceAtLeast(0.0)
    return bankedProfit + preciseProduction * currentMinutes
}

// 생산 배율로부터 정상 상태 시세 보정을 계산 — 강화투자액은 PLANET_EVENT_MARKET_UPGRADE_INVESTMENT_RATIO만큼만 흔들린다
fun Planet.marketAdjustmentFor(multiplier: Double): Long {
    val deviation = multiplier - 1.0
    return (deviation * buyPrice + deviation * upgradeInvestment * GameConstants.PLANET_EVENT_MARKET_UPGRADE_INVESTMENT_RATIO)
        .roundToLong()
        .coerceAtLeast(-(buyPrice + upgradeInvestment))
}

// 매도가·랭킹에 쓰이는 행성 시장가치. 항상 이 값을 통해서만 가치를 계산한다
// (buyPrice + upgradeInvestment를 직접 더하지 말 것). 정상 시세에 고장 중이면 할인을 얹는다
// (손해 배율 1.0 → −30%, 2.0 → −60%). 0 밑으로는 안 내려간다 — 파는데 코인을 더 내는 건 말이 안 됨
val Planet.marketValue: Long
    get() {
        val normal = (buyPrice + upgradeInvestment + marketAdjustment).coerceAtLeast(0L)
        if (!isBroken) return normal
        val discount = (GameConstants.PLANET_BROKEN_SELL_DISCOUNT_PER_LOSS * lossMultiplier).coerceAtMost(1.0)
        return (normal * (1.0 - discount)).roundToLong().coerceAtLeast(0L)
    }

// 투자액(매입가 + 강화비) 대비 현재 매도가의 차이 — 화면의 "시세 변동" 표시용
val Planet.marketChange: Long
    get() = marketValue - (buyPrice + upgradeInvestment)
