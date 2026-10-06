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

    val level: Int = 1,
    val totalProfit: Long = 0L,

    val variantId: String = "",

    // 강화에 투자한 누적 코인 (매도 가격 산정에 사용)
    val upgradeInvestment: Long = 0L,

    val lastProfitTime: Long = System.currentTimeMillis(),

    // 행성 시세/이벤트 시스템 — 이벤트가 터질 때마다 덮어써짐(누적 곱 아님), 매도로만 정리 가능
    val productionMultiplier: Double = 1.0,
    val marketAdjustment: Long = 0L,
    val lastEventTime: Long = System.currentTimeMillis(),

    // 이벤트로 생산량이 바뀌기 직전까지 쌓였지만 아직 수령 안 한 코인과, 그 적립이 어느 시점까지의
    // 몫인지(0 = 적립 없음). 앱을 안 연 사이 백그라운드에서 이벤트가 터져도 바뀌기 전 시간은 바뀌기
    // 전 생산량으로 쳐주기 위함 — 없으면 수령 시 전체 방치 시간이 새 생산량으로 소급 계산된다
    val bankedProfit: Double = 0.0,
    val bankedUntil: Long = 0L
)

// 강화 레벨이 오를수록 실제 생산량도 함께 오르도록 보정한 값 (분당 정수 표시용 — UI에 그대로 노출).
// productionMultiplier(이벤트로 흔들리는 배율)도 곱해져 온라인/오프라인 상관없이 항상 실제
// 수령액과 일치 — PLANET_PRODUCTION_SCALE·레벨 배율 같은 튜닝된 공식 자체는 안 건드림
val Planet.effectiveProduction: Long
    get() = preciseProduction.toLong()

// 실제 수익 정산(경과 시간 × 생산량)에 쓰는 내림하지 않은 값. effectiveProduction은 표시용으로
// 미리 내림된 값이라, 이걸 경과 분(최대 1440분)에 곱해 정산하면 소수점 손실이 누적돼 플레이어가
// 최대 수 % 손해를 본다 — 정산은 항상 이 값을 쓰고, 최종 합계에서만 한 번 내림한다
val Planet.preciseProduction: Double
    get() = preciseProductionAtLevel(level)

// 주어진 레벨에서의 분당 생산량(강화 미리보기용으로 다음 레벨 값도 여기서 구한다).
// 악재가 겹쳐 productionMultiplier가 음수(실손해)일 때 레벨 배율을 그대로 곱하면 강화할수록
// 손해가 커진다(-5 → -6 → -7). 손해 구간에선 레벨 배율로 나눠, 강화가 언제나 생산을 개선하도록 한다
// (-5 → -4.5 → -4.2 …). 배율 0 지점에서 두 식이 모두 0이라 값이 끊기지 않는다.
fun Planet.preciseProductionAtLevel(level: Int): Double {
    val levelMultiplier = GameConstants.planetLevelMultiplier(level)
    val base = production * GameConstants.PLANET_PRODUCTION_SCALE * productionMultiplier
    return if (base >= 0) base * levelMultiplier else base / levelMultiplier
}

// 마지막 수령 이후 정산 대상인 경과 분(정수). 오래 비워도 MAX_OFFLINE_MINUTES까지만 쳐준다.
fun Planet.elapsedProfitMinutes(now: Long): Long =
    minOf((now - lastProfitTime) / 60_000L, GameConstants.MAX_OFFLINE_MINUTES)

// lastProfitTime부터 [time]까지의 분(소수, 상한 적용). 상한은 앞쪽부터 채운다 — 처음 24시간만 생산.
private fun Planet.cappedMinutesUntil(time: Long): Double =
    ((time - lastProfitTime) / 60_000.0).coerceIn(0.0, GameConstants.MAX_OFFLINE_MINUTES.toDouble())

private val Planet.currentRateSince: Long
    get() = if (bankedUntil > 0L) bankedUntil else lastProfitTime

// 생산량이 바뀌기 직전([time])까지의 미수령 수익 — 이벤트가 배율을 바꾸기 전에 이 값을 적립해 둔다
fun Planet.bankedProfitAt(time: Long): Double =
    bankedProfit + preciseProduction * (cappedMinutesUntil(time) - cappedMinutesUntil(currentRateSince))

// 지금 수령할 미수령 수익 = 적립분 + 마지막 생산량 변화 이후 현재 생산량으로 번 몫.
// 경과 분은 기존처럼 정수로 내림한 값 기준(수령 시 lastProfitTime을 now로 당기므로)
fun Planet.pendingProfitAt(now: Long): Double {
    val currentMinutes = (elapsedProfitMinutes(now) - cappedMinutesUntil(currentRateSince)).coerceAtLeast(0.0)
    return bankedProfit + preciseProduction * currentMinutes
}

// 매도가·랭킹에 쓰이는 행성 시장가치. marketAdjustment(이벤트로 흔들리는 시세)가 반영되므로
// 항상 이 값을 통해서만 가치를 계산한다 (buyPrice + upgradeInvestment를 직접 더하지 말 것).
// 악재가 아무리 겹쳐도 시세는 0 밑으로 안 내려간다 — 산 값보다 싸게 팔 순 있어도(손해), 파는데
// 코인을 더 내는 건 말이 안 됨. RollPlanetEventUseCase가 marketAdjustment를 -(매입가+강화액)에서
// 이미 막지만, 예전 테스트 데이터 등에 대비해 여기서도 0으로 한 번 더 clamp한다.
val Planet.marketValue: Long
    get() = (buyPrice + upgradeInvestment + marketAdjustment).coerceAtLeast(0L)