package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import javax.inject.Inject

class BuyPlanetUseCase @Inject constructor(
    private val repository: PlanetRepository
) {
    suspend operator fun invoke(planet: Planet) {
        repository.buyPlanet(planet)
    }
}