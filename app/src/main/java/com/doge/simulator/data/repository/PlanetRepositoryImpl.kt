package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.PlanetDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanetRepositoryImpl(
    private val dao: PlanetDao
) : PlanetRepository {

    override suspend fun buyPlanet(planet: Planet) {
        dao.insertPlanet(planet.toEntity())
    }

    override fun getOwnedPlanets(): Flow<List<Planet>> {
        return dao.getOwnedPlanets()
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun sellPlanet(planetId: String): Boolean =
        dao.deletePlanet(planetId) > 0

    override suspend fun updatePlanetProfit(planetId: String, totalProfit: Long, lastProfitTime: Long) {
        dao.updateProfit(planetId, totalProfit, lastProfitTime)
    }

    override suspend fun upgradePlanet(planetId: String, level: Int, upgradeInvestment: Long) {
        dao.upgradePlanet(planetId, level, upgradeInvestment)
    }

    override suspend fun updatePlanetEvent(planetId: String, productionMultiplier: Double, marketAdjustment: Long, lastEventTime: Long) {
        dao.updatePlanetEvent(planetId, productionMultiplier, marketAdjustment, lastEventTime)
    }
}
