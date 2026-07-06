package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.EventLog
import com.doge.simulator.domain.model.EventLogType
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.effectiveProduction
import com.doge.simulator.domain.repository.EventLogRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject
import kotlin.random.Random

class CollectProfitUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val eventLogRepository: EventLogRepository,
    private val resourceRepository: ResourceRepository
) {
    suspend operator fun invoke(planets: List<Planet>) {
        val now = System.currentTimeMillis()
        var totalEarned = 0L

        planets.forEach { planet ->
            val rawElapsed = (now - planet.lastProfitTime) / 60_000L
            val elapsedMinutes = minOf(rawElapsed, GameConstants.MAX_OFFLINE_MINUTES)
            if (elapsedMinutes <= 0) return@forEach

            val earned = planet.effectiveProduction * elapsedMinutes
            totalEarned += earned

            planetRepository.updatePlanetProfit(
                planetId = planet.id,
                totalProfit = planet.totalProfit + earned,
                lastProfitTime = now
            )

            val meta = PlanetMetaDataTable.data[planet.type]
            val rarityMultiplier = GameConstants.RARITY_RESOURCE_MULTIPLIER[meta?.rarity] ?: 1.0
            val levelMultiplier = GameConstants.planetLevelMultiplier(planet.level)

            val droppedResources = meta?.resourceDrops.orEmpty().mapNotNull { (type, baseDropChance) ->
                val amount = rollResourceAmount(elapsedMinutes, baseDropChance, rarityMultiplier, levelMultiplier)
                if (amount <= 0L) return@mapNotNull null
                resourceRepository.add(type, amount)
                type to amount
            }
            val resourceText = droppedResources.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { (type, amount) -> "${type.displayName} +$amount" }
                ?.let { " | $it" }
                ?: ""

            eventLogRepository.insertLog(
                EventLog(
                    type = EventLogType.MAINTENANCE_COLLECTED,
                    planetId = planet.id,
                    planetType = planet.type.name,
                    planetDisplayName = meta?.displayName,
                    description = "${meta?.displayName ?: planet.type.name}: ${elapsedMinutes}분 수익 +$earned 코인$resourceText",
                    coinDelta = earned
                )
            )
        }

        if (totalEarned > 0) userRepository.addCoins(totalEarned)
    }

    // 분당 dropChance(%) × 등급 배율 × 강화 레벨 배율을 경과 시간에 대한 기댓값으로 환산하여 정수 개수를 산출
    private fun rollResourceAmount(
        elapsedMinutes: Long,
        baseDropChancePercent: Int,
        rarityMultiplier: Double,
        levelMultiplier: Double
    ): Long {
        val effectiveChance = baseDropChancePercent * rarityMultiplier * levelMultiplier
        val expected = elapsedMinutes * effectiveChance / 100.0
        val whole = expected.toLong()
        val fractional = expected - whole
        return whole + if (Random.nextDouble() < fractional) 1L else 0L
    }
}
