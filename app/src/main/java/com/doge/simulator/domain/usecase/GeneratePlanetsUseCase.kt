package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType
import javax.inject.Inject
import kotlin.random.Random

class GeneratePlanetsUseCase @Inject constructor() {

    suspend operator fun invoke(): Planet = generateRandomPlanet()

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
