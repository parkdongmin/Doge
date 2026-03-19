package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType

class GenerateInitialPlanetsUseCase {

    operator fun invoke(): List<Planet> {
        return PlanetType.entries.map { type ->
            val meta = PlanetMetaDataTable.data[type]!!

            Planet(
                type = type,
                buyPrice = meta.baseInvestmentCost,   // 초기 구매 가격
                currentValue = meta.baseInvestmentCost // 초기 시세
            )
        }
    }

    private fun calculateBasePrice(type: PlanetType): Int {
        return when (type) {
            PlanetType.TERRAN_WET -> 1200
            PlanetType.TERRAN_DRY -> 1000
            PlanetType.ISLANDS -> 900
            PlanetType.NO_ATMOSPHERE -> 600
            PlanetType.GAS_GIANT_1 -> 1500
            PlanetType.GAS_GIANT_2 -> 1300
            PlanetType.ICE_WORLD -> 800
            PlanetType.LAVA_WORLD -> 1100
            PlanetType.ASTEROID -> 300
            PlanetType.BLACK_HOLE -> 3000
            PlanetType.GALAXY -> 5000
            PlanetType.STAR -> 4000
        }
    }

    private fun calculateRisk(type: PlanetType): Int {
        return when (type) {
            PlanetType.TERRAN_WET -> 1
            PlanetType.TERRAN_DRY -> 2
            PlanetType.ISLANDS -> 1
            PlanetType.NO_ATMOSPHERE -> 3
            PlanetType.GAS_GIANT_1 -> 4
            PlanetType.GAS_GIANT_2 -> 2
            PlanetType.ICE_WORLD -> 3
            PlanetType.LAVA_WORLD -> 4
            PlanetType.ASTEROID -> 2
            PlanetType.BLACK_HOLE -> 5
            PlanetType.GALAXY -> 1
            PlanetType.STAR -> 2
        }
    }
}