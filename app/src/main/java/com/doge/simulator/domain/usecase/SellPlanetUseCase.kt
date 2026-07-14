package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

class SellPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(planet: Planet) {
        val baseValue = planet.buyPrice + planet.upgradeInvestment
        val fee = (baseValue * GameConstants.SELL_FEE_RATE).toLong()
        val proceeds = baseValue - fee

        planetRepository.sellPlanet(planet.id)
        userRepository.addCoins(proceeds)
    }
}
