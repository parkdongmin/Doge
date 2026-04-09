package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOwnedPlanetsUseCase @Inject constructor(
    private val repository: PlanetRepository
) {
    operator fun invoke(): Flow<List<Planet>> {
        return repository.getOwnedPlanets()
    }
}