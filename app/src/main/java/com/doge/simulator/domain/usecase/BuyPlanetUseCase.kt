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
        // 도감 등록 + 특성 행성이면 획득 기록(T12~T15 해금 조건) +1 — 매도해도 줄지 않는다
        userRepository.recordPlanetAcquisition(planet)
        return true
    }
}
