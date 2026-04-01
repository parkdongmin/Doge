package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType
import kotlin.random.Random

class GeneratePlanetsUseCase {

    operator fun invoke(): Planet {
        val type = getType()
        val meta = PlanetMetaDataTable.data[type]!!

        val production = (meta.productionMin..meta.productionMax).random()
        val risk = (meta.riskMin..meta.riskMax).random()
        val investment = (meta.investmentMin..meta.investmentMax).random()
        val eventRate = (meta.eventRateMin..meta.eventRateMax).random()

        val buyPrice =
            meta.basePrice +
                    (production * 20) +
                    (risk * 10)

        return Planet(
            type = type,
            production = production,
            risk = risk,
            investment = investment,
            eventRate = eventRate,
            buyPrice = buyPrice,
            currentValue = buyPrice
        )
    }

    private fun getType(): PlanetType {
        val roll = Random.nextInt(100) // 0~99

        return when {
            roll < 60 -> getRandomCommon()
            roll < 90 -> getRandomUncommon()  // 60~89 = 30%
            roll < 95 -> PlanetType.BLACK_HOLE // 90~94 = 5%
            roll < 99 -> PlanetType.GALAXY // 90~94 = 5%
            else -> PlanetType.STAR      // 99 = 1%
        }
    }

    private fun getRandomCommon(): PlanetType {
        val list = listOf(
            PlanetType.TERRAN_WET,
            PlanetType.TERRAN_DRY,
            PlanetType.ISLANDS,
            PlanetType.NO_ATMOSPHERE
        )
        return list.random()
    }

    private fun getRandomUncommon(): PlanetType {
        val list = listOf(
            PlanetType.GAS_GIANT_1,
            PlanetType.GAS_GIANT_2,
            PlanetType.ICE_WORLD,
            PlanetType.LAVA_WORLD,
            PlanetType.ASTEROID
        )
        return list.random()
    }

}