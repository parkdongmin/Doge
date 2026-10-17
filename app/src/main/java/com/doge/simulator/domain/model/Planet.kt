package com.doge.simulator.domain.model

import java.util.UUID
import kotlin.math.roundToInt
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
    val lossMultiplier: Double = 0.0,

    // 2부 행성 특성(T11 이상에서 발견한 행성만). 효과는 traitEffects 확장으로만 계산한다
    val traits: List<PlanetTrait> = emptyList()
)

val Planet.hasTraits: Boolean
    get() = traits.isNotEmpty()

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
    production * GameConstants.PLANET_PRODUCTION_SCALE * GameConstants.planetLevelMultiplier(level) * productionMultiplier *
        traitEffects.productionMultiplier

// ── 특성 효과 ────────────────────────────────────────────────────────
// 특성이 바꾸는 값은 전부 아래 확장으로만 읽는다. 수령·오프라인 정산·이벤트·정비·강화·화면이 같은 값을 쓰게 —
// 원시 값(eventRate, UPGRADE_SUCCESS_RATES, PLANET_BREAKDOWN_DELTA_THRESHOLD)을 직접 읽으면 특성이 빠진다
data class TraitEffects(
    val productionMultiplier: Double = 1.0,
    val badChanceDeltaPercent: Double = 0.0,
    val breakdownThresholdDelta: Double = 0.0,
    val boomMultiplier: Double = 1.0,
    val resourceDropMultiplier: Double = 1.0,
    val upgradeSuccessMultiplier: Double = 1.0,
    // 구매가에 이미 반영된 특성 시세 보정(0.35 = +35%) — 발견 화면 "특성 시세" 표시용
    val priceAdjustment: Double = 0.0
)

// 반대·중복 금지 규칙으로 한 축에는 특성이 최대 1개라, 합·곱이 실제로 섞이는 경우는 없다
val Planet.traitEffects: TraitEffects
    get() {
        if (traits.isEmpty()) return NO_TRAIT_EFFECTS
        var effects = TraitEffects()
        for (trait in traits) {
            val value = PlanetTraitTable.value(type, trait)
            effects = when (trait.type.axis) {
                TraitAxis.PRODUCTION -> effects.copy(productionMultiplier = effects.productionMultiplier * (1.0 + value))
                // 안정된 환경은 표에 양수(줄일 %p)로, 불안정한 환경은 늘릴 %p로 들어 있다
                TraitAxis.BAD_CHANCE -> effects.copy(
                    badChanceDeltaPercent = effects.badChanceDeltaPercent + if (trait.type.isGood) -value else value
                )
                TraitAxis.BREAKDOWN_THRESHOLD -> effects.copy(breakdownThresholdDelta = effects.breakdownThresholdDelta + value)
                TraitAxis.BOOM -> effects.copy(boomMultiplier = effects.boomMultiplier * (1.0 + value))
                // 풍부한 자원은 +비율(1.5 = ×2.5), 메마른 자원은 배율 그대로(×0.5)
                TraitAxis.RESOURCE_DROP -> effects.copy(
                    resourceDropMultiplier = effects.resourceDropMultiplier * if (trait.type.isGood) 1.0 + value else value
                )
                TraitAxis.UPGRADE_SUCCESS -> effects.copy(upgradeSuccessMultiplier = effects.upgradeSuccessMultiplier * value)
            }
            effects = effects.copy(priceAdjustment = effects.priceAdjustment + PlanetTraitTable.priceEffect(type, trait))
        }
        return effects
    }

private val NO_TRAIT_EFFECTS = TraitEffects()

// 이벤트가 악재로 나올 확률(%) — 기본 eventRate에 특성 %p를 더한 값
val Planet.effectiveBadChance: Int
    get() = (eventRate + traitEffects.badChanceDeltaPercent).roundToInt().coerceIn(0, 100)

// 정상 행성이 고장 나는 악재폭 기준
val Planet.breakdownThreshold: Double
    get() = GameConstants.PLANET_BREAKDOWN_DELTA_THRESHOLD + traitEffects.breakdownThresholdDelta

// 정상 상태에서 호재가 뜰 때 상승폭에 곱하는 배율
val Planet.boomMultiplier: Double
    get() = traitEffects.boomMultiplier

val Planet.resourceDropMultiplier: Double
    get() = traitEffects.resourceDropMultiplier

// 지금 레벨에서 강화 1회 성공률. 특성으로 올라도 99%에서 멈춘다(확정 성공은 없음)
fun Planet.upgradeSuccessRate(): Float =
    ((GameConstants.UPGRADE_SUCCESS_RATES[level] ?: 0f) * traitEffects.upgradeSuccessMultiplier)
        .toFloat().coerceAtMost(MAX_UPGRADE_SUCCESS_RATE)

private const val MAX_UPGRADE_SUCCESS_RATE = 0.99f

val Planet.normalProduction: Double
    get() = normalProductionAtLevel(level)

val Planet.rarity: RarityTier
    get() = PlanetMetaDataTable.data[type]?.rarity ?: RarityTier.COMMON

// 현재 레벨에서 다음 레벨로 1회 강화 시도할 때의 비용 (코인, 자원)
val Planet.upgradeCost: Pair<Long, Map<ResourceType, Int>>
    get() = GameConstants.planetUpgradeCost(level, rarity)

// 정비 코인 비용 — 정상일 때 생산량의 90분치
val Planet.maintenanceCost: Long
    get() = (normalProduction * GameConstants.PLANET_MAINTENANCE_COST_MINUTES).toLong().coerceAtLeast(1L)

// 정비 자원 비용 — 등급별 기본량 × 강화 레벨 배율²
val Planet.maintenanceResourceCost: Map<ResourceType, Long>
    get() {
        val levelMultiplier = GameConstants.planetLevelMultiplier(level)
        return GameConstants.PLANET_MAINTENANCE_RESOURCE_BASE[rarity].orEmpty()
            .mapValues { (_, base) -> (base * levelMultiplier * levelMultiplier).roundToLong() }
    }

// 보유 자원 기준 정비 계산. 있는 만큼은 자원으로 쓰고, 모자란 몫은 (판매 단가 × 대체 배율) 코인으로 낸다
data class MaintenancePlan(
    val resourcesUsed: Map<ResourceType, Long>,
    val shortfall: Map<ResourceType, Long>,
    val baseCoins: Long,
    val substituteCoins: Long
) {
    val totalCoins: Long get() = baseCoins + substituteCoins
}

fun Planet.maintenancePlan(owned: (ResourceType) -> Long): MaintenancePlan {
    val used = mutableMapOf<ResourceType, Long>()
    val shortfall = mutableMapOf<ResourceType, Long>()
    var substitute = 0L
    for ((type, need) in maintenanceResourceCost) {
        val use = minOf(need, owned(type).coerceAtLeast(0L))
        if (use > 0) used[type] = use
        val missing = need - use
        if (missing > 0) {
            shortfall[type] = missing
            val unitPrice = GameConstants.RESOURCE_SELL_PRICE[type] ?: 0L
            substitute += (missing * unitPrice * GameConstants.PLANET_MAINTENANCE_RESOURCE_COIN_RATE).roundToLong()
        }
    }
    return MaintenancePlan(used, shortfall, maintenanceCost, substitute)
}

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

// 적합도 높음인 좋은 특성 중 가장 높은 등급(없으면 0) — T13(Ⅱ 이상)·T15(Ⅲ) 해금 기록 판정용
val Planet.highFitGrade: Int
    get() = traits.filter { it.type.isGood && PlanetTraitTable.fit(type, it.type) == TraitFit.HIGH }
        .maxOfOrNull { it.intensity } ?: 0
