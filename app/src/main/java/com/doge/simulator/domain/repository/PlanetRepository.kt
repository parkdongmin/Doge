package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Planet
import kotlinx.coroutines.flow.Flow

interface PlanetRepository {

    suspend fun buyPlanet(planet: Planet)

    fun getOwnedPlanets(): Flow<List<Planet>>

    // 반환값이 false면 이미 삭제된 행성(중복 매도 시도)이라는 뜻
    suspend fun sellPlanet(planetId: String): Boolean

    suspend fun updatePlanetProfit(planetId: String, totalProfit: Long, lastProfitTime: Long)

    suspend fun upgradePlanet(planetId: String, level: Int, upgradeInvestment: Long)

    suspend fun updatePlanetEvent(planetId: String, productionMultiplier: Double, marketAdjustment: Long, lastEventTime: Long)
}
