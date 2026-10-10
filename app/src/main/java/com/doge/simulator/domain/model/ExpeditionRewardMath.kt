package com.doge.simulator.domain.model

import kotlin.math.ceil

// 탐사 보상·소요 시간 계산식 모음. 실제 지급(StartExpeditionUseCase·CompleteExpeditionUseCase)과
// 코인 단축 비용(기대값 기준)이 같은 식을 쓰도록 한곳에 둔다 — 한쪽만 바뀌면 단축이 이득이 되는
// 구멍이 생길 수 있다. ExpeditionEconomyTest·CoinSkipCostTest가 이 함수들로 검증한다

// 우주선 속도로 줄어든 실제 탐사 시간
fun expeditionDurationMs(tier: Int, spaceship: Spaceship): Long {
    val baseMinutes = GameConstants.EXPEDITION_BASE_MINUTES[tier] ?: 20L
    return (baseMinutes * 60_000L * (1.0 - spaceship.durationReduction)).toLong()
        .coerceAtLeast(GameConstants.EXPEDITION_MIN_DURATION_MS)
}

// 성공 시 자원 종류마다 곱해지는 배율(기본 랜덤 1~5개에 곱함) — 적재량·인원·분야 숙련도 합·소요 시간·티어
fun expeditionResourceMultiplier(
    tier: Int,
    spaceship: Spaceship?,
    astronauts: List<Astronaut>,
    category: ExpeditionCategory
): Double {
    val cargoMultiplier = 1.0 + (spaceship?.cargoBonus ?: 0.0)
    val crewMultiplier = 1.0 + (astronauts.size - 1).coerceAtLeast(0) * GameConstants.CREW_SIZE_RESOURCE_BONUS_PER_HEAD
    val proficiencySum = astronauts.filter { it.specialty.relatedCategory == category }.sumOf { it.proficiency }
    val specialtyMultiplier = 1.0 + (proficiencySum / 100.0) * GameConstants.SPECIALTY_PROFICIENCY_RESOURCE_COEFFICIENT
    val tierMinutes = GameConstants.EXPEDITION_BASE_MINUTES[tier]
        ?: GameConstants.EXPEDITION_BASE_MINUTES.getValue(GameConstants.EXPEDITION_BASE_MINUTES.keys.max())
    val durationMultiplier = tierMinutes / GameConstants.EXPEDITION_RESOURCE_REFERENCE_MINUTES
    return durationMultiplier * GameConstants.expeditionTierMultiplier(tier) *
        cargoMultiplier * crewMultiplier * specialtyMultiplier
}

// 성공한 탐사에서 행성을 발견할 확률
fun expeditionDiscoveryChance(lab: ResearchLab, category: ExpeditionCategory): Float {
    val baseChance = GameConstants.PLANET_DISCOVERY_BASE_CHANCE +
        if (category == ExpeditionCategory.PLANET) GameConstants.PLANET_DISCOVERY_PLANET_CATEGORY_BONUS else 0f
    val celestialBonus = lab.celestialAnalysisLevel * GameConstants.PLANET_DISCOVERY_CELESTIAL_BONUS_PER_LEVEL
    return (baseChance + celestialBonus).coerceIn(0f, 0.8f)
}

// 티어별 발견 행성의 기대 구매가 — 등급 가중치 × 등급 내 종류 균등(rollPlanetType),
// 가격식은 발견 시 매기는 basePrice + production×20 + risk×10의 범위 중앙값
fun expectedDiscoveredPlanetPrice(tier: Int): Double {
    val weights = GameConstants.PLANET_RARITY_WEIGHTS[tier]
        ?: GameConstants.PLANET_RARITY_WEIGHTS.getValue(GameConstants.PLANET_RARITY_WEIGHTS.keys.max())
    val byRarity = PlanetMetaDataTable.data.values.groupBy { it.rarity }
    return weights.entries.sumOf { (rarity, weight) ->
        val metas = byRarity[rarity].orEmpty()
        if (metas.isEmpty()) 0.0 else weight / 100.0 * metas.map { m ->
            m.basePrice + (m.productionMin + m.productionMax) / 2.0 * 20 + (m.riskMin + m.riskMax) / 2.0 * 10
        }.average()
    }
}

// 탐사 1회의 기대 가치(코인 환산): 성공 확률 × (기본 코인 + 자원 판매가 + 행성 발견 가치).
// 발견 가치는 슬롯 여부와 무관하게 "코인으로 받기"(중복 기준 30% × 티어 배율)로 잡는다 — 슬롯이 남을 땐
// 실제로는 그 행성을 사서 계속 버는 가치가 더 크지만, 이를 보수적으로 깔고 단축 배율(2배)로 덮는다
fun expectedExpeditionValue(
    tier: Int,
    spaceship: Spaceship?,
    astronauts: List<Astronaut>,
    category: ExpeditionCategory,
    lab: ResearchLab
): Double {
    val success = expeditionSuccessChance(spaceship, astronauts, category).toDouble()
    val multiplier = expeditionResourceMultiplier(tier, spaceship, astronauts, category)
    val averageBaseAmount = 3.0 // Random.nextLong(1, 6)의 평균
    val resourceValue = ResourceType.entries.filter { it.category == category }.sumOf {
        averageBaseAmount * multiplier * (GameConstants.RESOURCE_SELL_PRICE[it] ?: 0L)
    }
    val discoveryValue = expeditionDiscoveryChance(lab, category) * expectedDiscoveredPlanetPrice(tier) *
        GameConstants.DUPLICATE_PLANET_VARIANT_COIN_RATE * GameConstants.slotFullCoinTierScale(tier)
    return success * (GameConstants.expeditionSuccessCoinReward(tier) + resourceValue + discoveryValue)
}

// 코인 단축의 분당 단가 = max(행성 방치 분당 수입, 이 탐사의 분당 기대 가치). 방치 수입에 묶어
// 성장 단계와 상관없이 "1분 단축 = 내 수입 몇 분치"로 체감이 같고, 탐사 가치 이상이라 단축이
// 이득이 되는 일은 없다 (배율은 GameConstants.COIN_SKIP_PREMIUM)
fun coinSkipUnitPerMinute(
    expedition: Expedition,
    spaceship: Spaceship?,
    astronauts: List<Astronaut>,
    lab: ResearchLab,
    passivePerMinute: Double
): Double {
    val nominalMinutes = (spaceship?.let { expeditionDurationMs(expedition.tier, it) }
        ?: ((GameConstants.EXPEDITION_BASE_MINUTES[expedition.tier] ?: 20L) * 60_000L)) / 60_000.0
    val valuePerMinute = expectedExpeditionValue(expedition.tier, spaceship, astronauts, expedition.category, lab) / nominalMinutes
    return maxOf(passivePerMinute.coerceAtLeast(0.0), valuePerMinute)
}

fun coinSkipCost(remainingMs: Long, unitPerMinute: Double): Long =
    ceil(remainingMs / 60_000.0 * unitPerMinute * GameConstants.COIN_SKIP_PREMIUM).toLong().coerceAtLeast(1L)
