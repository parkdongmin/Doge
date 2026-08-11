package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

class UpgradeSpaceshipUseCase @Inject constructor(
    private val spaceshipRepository: SpaceshipRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository
) {
    sealed class Result {
        object Success : Result()
        object InsufficientCoins : Result()
        object InsufficientResources : Result()
        object MaxGradeReached : Result()
        object Conflict : Result()
    }

    suspend operator fun invoke(spaceship: Spaceship): Result {
        if (spaceship.grade >= GameConstants.MAX_SPACESHIP_GRADE) return Result.MaxGradeReached

        val (coinCost, resourceCost) = GameConstants.spaceshipUpgradeCost(spaceship.grade)

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

        val newGrade = spaceship.grade + 1
        val applied = spaceshipRepository.upgrade(
            id = spaceship.id,
            expectedGrade = spaceship.grade,
            grade = newGrade,
            crewCapacity = minOf(GameConstants.MAX_CREW_CAPACITY, spaceship.crewCapacity + GameConstants.UPGRADE_CREW_PER_GRADE),
            speed = minOf(100, spaceship.speed + GameConstants.UPGRADE_SPEED_PER_GRADE),
            cargo = minOf(100, spaceship.cargo + GameConstants.UPGRADE_CARGO_PER_GRADE),
            successRate = minOf(0.95f, spaceship.successRate + GameConstants.UPGRADE_SUCCESS_RATE_PER_GRADE)
        )
        if (!applied) {
            // 그사이 다른 강화가 먼저 반영됨 — 지불한 비용 환불
            userRepository.addCoins(coinCost)
            for ((refundType, refundAmount) in consumed) resourceRepository.add(refundType, refundAmount)
            return Result.Conflict
        }
        return Result.Success
    }
}
