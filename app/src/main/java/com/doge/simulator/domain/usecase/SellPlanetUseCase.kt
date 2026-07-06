package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.EventLog
import com.doge.simulator.domain.model.EventLogType
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.repository.EventLogRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

class SellPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val eventLogRepository: EventLogRepository
) {
    suspend operator fun invoke(planet: Planet) {
        val baseValue = planet.buyPrice + planet.upgradeInvestment
        val fee = (baseValue * GameConstants.SELL_FEE_RATE).toLong()
        val proceeds = baseValue - fee

        planetRepository.sellPlanet(planet.id)
        userRepository.addCoins(proceeds)

        val meta = PlanetMetaDataTable.data[planet.type]
        eventLogRepository.insertLog(
            EventLog(
                type = EventLogType.PLANET_SOLD,
                planetId = planet.id,
                planetType = planet.type.name,
                planetDisplayName = meta?.displayName,
                description = "${meta?.displayName ?: planet.type.name} 매도 (+${"%,d".format(proceeds)} 코인, 수수료 ${"%,d".format(fee)})",
                valueBefore = baseValue.toInt(),
                coinDelta = proceeds
            )
        )
    }
}
