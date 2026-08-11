package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BuySpaceshipUseCase @Inject constructor(
    private val spaceshipRepository: SpaceshipRepository,
    private val userRepository: UserRepository,
    private val researchLabRepository: ResearchLabRepository
) {
    sealed class Result {
        object Success : Result()
        object InsufficientCoins : Result()
        object MaxLimitReached : Result()
    }

    suspend operator fun invoke(): Result {
        val lab = researchLabRepository.get().first()
        val ships = spaceshipRepository.getSpaceships().first()
        if (ships.size >= lab.maxSpaceships) return Result.MaxLimitReached

        if (!userRepository.deductCoins(GameConstants.SCOUT_SHIP_BASE_COST)) return Result.InsufficientCoins
        spaceshipRepository.buy(
            Spaceship(
                name = "정찰선 MK-${ships.size + 1}",
                grade = 1,
                crewCapacity = GameConstants.SCOUT_CREW_BASE,
                speed = GameConstants.SCOUT_SPEED_BASE,
                cargo = GameConstants.SCOUT_CARGO_BASE,
                successRate = GameConstants.SCOUT_SUCCESS_RATE_BASE
            )
        )
        return Result.Success
    }
}
