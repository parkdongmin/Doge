package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.PlanetDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
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

    override suspend fun sellPlanet(planetId: String) {
        dao.deletePlanet(planetId)
    }

    override suspend fun updatePlanetValue(planetId: String, newValue: Int) {
        dao.updateValue(planetId, newValue)
    }
}