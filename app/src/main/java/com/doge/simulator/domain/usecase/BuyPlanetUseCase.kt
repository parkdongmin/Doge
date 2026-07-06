package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.EventLog
import com.doge.simulator.domain.model.EventLogType
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.repository.EventLogRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

class BuyPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val eventLogRepository: EventLogRepository
) {
    suspend operator fun invoke(planet: Planet): Boolean {
        val success = userRepository.deductCoins(planet.buyPrice.toLong())
        if (!success) return false

        planetRepository.buyPlanet(planet)
        userRepository.recordVariantDiscovery(planet.variantId)

        val meta = PlanetMetaDataTable.data[planet.type]
        eventLogRepository.insertLog(
            EventLog(
                type = EventLogType.PLANET_BOUGHT,
                planetId = planet.id,
                planetType = planet.type.name,
                planetDisplayName = meta?.displayName,
                description = "${meta?.displayName ?: planet.type.name} 매입 (-${"%,d".format(planet.buyPrice)} 코인)",
                coinDelta = -planet.buyPrice.toLong()
            )
        )
        return true
    }
}
