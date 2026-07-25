package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class UpgradePlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository
) {
    sealed class Result {
        data class Success(val newLevel: Int) : Result()
        // previousLevel/investment: 광고 시청으로 되돌릴 때 필요한 강화 전 레벨과, 이미 반영된 투자액
        // (실패해도 재료는 환불되지 않으므로 되돌리기는 레벨만 복구하고 investment는 그대로 유지)
        data class Failed(
            val levelDropped: Boolean,
            val currentLevel: Int,
            val previousLevel: Int,
            val investment: Long
        ) : Result()
        object MaxLevel : Result()
        object InsufficientCoins : Result()
        object InsufficientResources : Result()
    }

    suspend operator fun invoke(planet: Planet): Result {
        if (planet.level >= GameConstants.PLANET_MAX_LEVEL) return Result.MaxLevel

        val (coinCost, resourceCost) = GameConstants.planetUpgradeCost(planet.level)
        val coins = userRepository.getCoins().first()
        if (coins < coinCost) return Result.InsufficientCoins

        for ((type, amount) in resourceCost) {
            if (resourceRepository.getAmount(type) < amount) return Result.InsufficientResources
        }

        // 비용 차감
        userRepository.addCoins(-coinCost)
        for ((type, amount) in resourceCost) resourceRepository.consume(type, amount.toLong())

        val totalInvestment = planet.upgradeInvestment + coinCost

        // 성공률 계산
        val successRate = GameConstants.UPGRADE_SUCCESS_RATES[planet.level] ?: 0.5f
        val isSuccess = Random.nextFloat() < successRate

        return if (isSuccess) {
            val newLevel = planet.level + 1
            planetRepository.upgradePlanet(planet.id, newLevel, totalInvestment)
            Result.Success(newLevel)
        } else {
            val isDangerZone = planet.level >= GameConstants.DANGER_ZONE_START
            val newLevel = if (isDangerZone) maxOf(1, planet.level - 1) else planet.level
            planetRepository.upgradePlanet(planet.id, newLevel, totalInvestment)
            Result.Failed(
                levelDropped = isDangerZone,
                currentLevel = newLevel,
                previousLevel = planet.level,
                investment = totalInvestment
            )
        }
    }
}
