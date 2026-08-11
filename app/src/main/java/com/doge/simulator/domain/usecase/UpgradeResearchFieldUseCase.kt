package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.model.ResearchLab
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpgradeResearchFieldUseCase @Inject constructor(
    private val researchLabRepository: ResearchLabRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository
) {
    sealed class Result {
        data class Success(val newLevel: Int) : Result()
        object InsufficientCoins : Result()
        object InsufficientResources : Result()
        object MaxLevelReached : Result()
        object Conflict : Result()
    }

    suspend operator fun invoke(field: ResearchField): Result {
        val lab = researchLabRepository.get().first()
        val currentLevel = lab.getLevel(field)

        val maxLevel = ResearchLab.maxLevel(field)
        if (maxLevel != null && currentLevel >= maxLevel) return Result.MaxLevelReached

        val (coinCost, resourceCost) = GameConstants.researchUpgradeCost(currentLevel)

        if (!userRepository.deductCoins(coinCost)) return Result.InsufficientCoins

        val consumed = mutableListOf<Pair<ResourceType, Long>>()
        for ((type, amount) in resourceCost) {
            if (!resourceRepository.consume(type, amount.toLong())) {
                userRepository.addCoins(coinCost)
                for ((refundType, refundAmount) in consumed) resourceRepository.add(refundType, refundAmount)
                return Result.InsufficientResources
            }
            consumed.add(type to amount.toLong())
        }

        val newLevel = currentLevel + 1
        val applied = researchLabRepository.upgradeField(field, currentLevel, newLevel)
        if (!applied) {
            // 그사이 다른 강화가 먼저 반영됨 — 지불한 비용 환불
            userRepository.addCoins(coinCost)
            for ((refundType, refundAmount) in consumed) resourceRepository.add(refundType, refundAmount)
            return Result.Conflict
        }
        return Result.Success(newLevel)
    }
}
