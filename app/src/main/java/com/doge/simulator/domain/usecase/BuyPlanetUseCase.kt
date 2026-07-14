package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BuyPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val researchLabRepository: ResearchLabRepository
) {
    suspend operator fun invoke(planet: Planet): Boolean {
        val lab = researchLabRepository.get().first()
        val ownedCount = planetRepository.getOwnedPlanets().first().size
        if (ownedCount >= lab.maxPlanetSlots) return false

        val success = userRepository.deductCoins(planet.buyPrice.toLong())
        if (!success) return false

        planetRepository.buyPlanet(planet)
        userRepository.recordVariantDiscovery(planet.variantId)
        return true
    }
}
