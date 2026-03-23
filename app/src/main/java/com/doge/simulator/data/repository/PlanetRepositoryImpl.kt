package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.PlanetDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.usecase.GenerateInitialPlanetsUseCase

class PlanetRepositoryImpl(
    private val dao: PlanetDao,
    private val generateInitialPlanetsUseCase: GenerateInitialPlanetsUseCase
) : PlanetRepository {

    override suspend fun getInitialPlanets(): List<Planet> {
        return generateInitialPlanetsUseCase() // UseCase 호출
    }

    override suspend fun buyPlanet(planet: Planet) {
        dao.insertPlanet(planet.toEntity())
    }

    override suspend fun getOwnedPlanets(): List<Planet> {
        return dao.getOwnedPlanets().map { it.toDomain() }
    }

    override suspend fun sellPlanet(planetId: String) {
        dao.deletePlanet(planetId)
    }

    override suspend fun updatePlanetValue(planetId: String, newValue: Int) {
        dao.updateValue(planetId, newValue)
    }
}