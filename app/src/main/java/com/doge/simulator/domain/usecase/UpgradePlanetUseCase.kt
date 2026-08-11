package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
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

        // 코인·자원 차감은 원자적 연산으로 수행 — 잔액/잔량 확인과 차감을 한 번에 처리해
        // 동시 요청(연타)이 있어도 이중 차감이나 음수 잔액이 발생하지 않는다
        if (!userRepository.deductCoins(coinCost)) return Result.InsufficientCoins

        val consumed = mutableListOf<Pair<ResourceType, Long>>()
        for ((type, amount) in resourceCost) {
            if (!resourceRepository.consume(type, amount.toLong())) {
                // 자원 부족으로 실패 — 이미 차감한 코인·자원 환불
                userRepository.addCoins(coinCost)
                for ((refundType, refundAmount) in consumed) resourceRepository.add(refundType, refundAmount)
                return Result.InsufficientResources
            }
            consumed.add(type to amount.toLong())
        }

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
