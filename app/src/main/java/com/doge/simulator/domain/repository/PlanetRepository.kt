package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Planet
import kotlinx.coroutines.flow.Flow

interface PlanetRepository {

    suspend fun buyPlanet(planet: Planet)

    fun getOwnedPlanets(): Flow<List<Planet>>

    suspend fun sellPlanet(planetId: String)

    suspend fun updatePlanetProfit(planetId: String, totalProfit: Long, lastProfitTime: Long)

    suspend fun upgradePlanet(planetId: String, level: Int, upgradeInvestment: Long)
}
