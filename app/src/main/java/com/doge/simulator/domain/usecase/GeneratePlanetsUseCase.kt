package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetTrait
import com.doge.simulator.domain.model.PlanetTraitRoller
import com.doge.simulator.domain.model.PlanetTraitTable
import com.doge.simulator.domain.model.PlanetType
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.random.Random

class GeneratePlanetsUseCase @Inject constructor() {

    // 신규 게임 스타터 행성 — 가장 소박한 COMMON(무대기 행성)을 최저 스탯으로 결정론적 생성.
    // 방치수익을 첫 진입부터 체감시키기 위한 "무료 첫 생산기".
    fun starterPlanet(): Planet {
        val type = PlanetType.NO_ATMOSPHERE
        val meta = PlanetMetaDataTable.data.getValue(type)
        val production = meta.productionMin
        val risk = meta.riskMin
        val eventRate = (meta.eventRateMin + meta.eventRateMax) / 2
        return Planet(
            type = type,
            production = production,
            risk = risk,
            investment = meta.investmentMin,
            eventRate = eventRate,
            variantId = meta.variants.first().variantId,
            buyPrice = discoveryPrice(type, production, risk, emptyList())
        )
    }

    // 탐사에서 발견한 행성 — 스탯·변종·특성·가격을 여기서 한 번에 확정한다. 탐사 완료 시점에 한 번만
    // 불러 결과를 저장해 두므로(CompleteExpeditionUseCase), 결과 창을 다시 띄워도 같은 행성이 나온다
    fun discoveredPlanet(type: PlanetType, tier: Int, random: Random = Random): Planet {
        val meta = PlanetMetaDataTable.data.getValue(type)
        val production = (meta.productionMin..meta.productionMax).random(random)
        val risk = (meta.riskMin..meta.riskMax).random(random)
        val traits = PlanetTraitRoller.roll(type, tier, random)
        return Planet(
            type = type,
            production = production,
            risk = risk,
            investment = (meta.investmentMin..meta.investmentMax).random(random),
            eventRate = (meta.eventRateMin..meta.eventRateMax).random(random),
            variantId = meta.variants.random(random).variantId,
            buyPrice = discoveryPrice(type, production, risk, traits),
            traits = traits
        )
    }

    // 티어별 등급(rarity) 가중치(GameConstants.PLANET_RARITY_WEIGHTS)로 등급을 먼저 정하고,
    // 그 등급에 속한 행성 종류들 사이에서는 균등 분배 — 고티어라도 커먼/언커먼이 완전히 배제되지 않고,
    // 레전더리/에픽 비중은 캡이 있어 "무조건 최상급"이 되지 않게 함
    fun rollPlanetType(tier: Int, random: Random = Random): PlanetType {
        val rarityWeights = GameConstants.PLANET_RARITY_WEIGHTS[tier]
            ?: GameConstants.PLANET_RARITY_WEIGHTS.getValue(GameConstants.PLANET_RARITY_WEIGHTS.keys.max())
        val typesByRarity = PlanetType.entries.groupBy { PlanetMetaDataTable.data.getValue(it).rarity }

        val weightedTypes = rarityWeights.flatMap { (rarity, rarityWeight) ->
            val typesInRarity = typesByRarity[rarity].orEmpty()
            typesInRarity.map { type -> type to rarityWeight / typesInRarity.size }
        }

        val totalWeight = weightedTypes.sumOf { it.second.toDouble() }
        var roll = random.nextDouble() * totalWeight
        for ((type, weight) in weightedTypes) {
            roll -= weight
            if (roll <= 0) return type
        }
        return weightedTypes.last().first
    }

    companion object {
        // 발견 가격 = (기본가 + 생산량×20 + 위험도×10) × (1 + 특성 시세 보정). 특성 시세를 구매가에 넣어 두면
        // 매도가·이벤트 시세·랭킹(marketValue)이 모두 buyPrice에서 출발하므로 따로 손볼 곳이 없다
        fun discoveryPrice(type: PlanetType, production: Int, risk: Int, traits: List<PlanetTrait>): Int {
            val meta = PlanetMetaDataTable.data.getValue(type)
            val base = meta.basePrice + production * 20 + risk * 10
            val adjustment = traits.sumOf { PlanetTraitTable.priceEffect(type, it) }
            return (base * (1.0 + adjustment)).roundToInt()
        }
    }
}
