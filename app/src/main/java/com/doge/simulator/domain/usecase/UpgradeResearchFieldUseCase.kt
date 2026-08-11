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
        researchLabRepository.upgradeField(field, newLevel)
        return Result.Success(newLevel)
    }
}
