package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType
import javax.inject.Inject
import kotlin.random.Random

class GeneratePlanetsUseCase @Inject constructor() {

    suspend operator fun invoke(): Planet = generateRandomPlanet()

    // 신규 게임 스타터 행성 — 가장 소박한 COMMON(무대기 행성)을 최저 스탯으로 결정론적 생성.
    // 방치수익을 첫 진입부터 체감시키기 위한 "무료 첫 생산기".
    fun starterPlanet(): Planet {
        val type = PlanetType.NO_ATMOSPHERE
        val meta = PlanetMetaDataTable.data.getValue(type)
        val production = meta.productionMin
        val risk = meta.riskMin
        val eventRate = (meta.eventRateMin + meta.eventRateMax) / 2
        val buyPrice = meta.basePrice + (production * 20) + (risk * 10)
        return Planet(
            type = type,
            production = production,
            risk = risk,
            investment = meta.investmentMin,
            eventRate = eventRate,
            variantId = meta.variants.first().variantId,
            buyPrice = buyPrice,
            currentValue = buyPrice
        )
    }

    private fun generateRandomPlanet(): Planet {
        val type = getType()
        val meta = PlanetMetaDataTable.data[type]!!

        val production = (meta.productionMin..meta.productionMax).random()
        val risk = (meta.riskMin..meta.riskMax).random()
        val investment = (meta.investmentMin..meta.investmentMax).random()
        val eventRate = (meta.eventRateMin..meta.eventRateMax).random()
        val buyPrice = meta.basePrice + (production * 20) + (risk * 10)
        val variant = meta.variants.random()

        return Planet(
            type = type,
            production = production,
            risk = risk,
            investment = investment,
            eventRate = eventRate,
            variantId = variant.variantId,
            buyPrice = buyPrice,
            currentValue = buyPrice
        )
    }

    private fun getType(): PlanetType {
        val roll = Random.nextInt(100)
        return when {
            roll < 60 -> getRandomCommon()
            roll < 90 -> getRandomUncommon()
            roll < 95 -> PlanetType.BLACK_HOLE
            roll < 99 -> PlanetType.GALAXY
            else -> PlanetType.STAR
        }
    }

    private fun getRandomCommon(): PlanetType = listOf(
        PlanetType.TERRAN_WET, PlanetType.TERRAN_DRY,
        PlanetType.ISLANDS, PlanetType.NO_ATMOSPHERE
    ).random()

    private fun getRandomUncommon(): PlanetType = listOf(
        PlanetType.GAS_GIANT_1, PlanetType.GAS_GIANT_2,
        PlanetType.ICE_WORLD, PlanetType.LAVA_WORLD, PlanetType.ASTEROID
    ).random()
}
