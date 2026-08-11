package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.TrainingType
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TrainAstronautUseCase @Inject constructor(
    private val astronautRepository: AstronautRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository,
    private val researchLabRepository: ResearchLabRepository
) {
    sealed class Result {
        object Success : Result()
        object InsufficientCoins : Result()
        object InsufficientResources : Result()
        object TrainingSlotFull : Result()
        object AstronautNotIdle : Result()
        object AlreadyAtCap : Result()
    }

    suspend operator fun invoke(astronaut: Astronaut, isAdvanced: Boolean): Result {
        if (astronaut.status != AstronautStatus.IDLE) return Result.AstronautNotIdle
        if (astronaut.proficiency >= astronaut.grade.proficiencyCap) return Result.AlreadyAtCap

        val lab = researchLabRepository.get().first()
        val trainingCount = astronautRepository.getAstronauts().first()
            .count { it.status == AstronautStatus.TRAINING }
        if (trainingCount >= lab.maxTrainingSlots) return Result.TrainingSlotFull

        val coinCost = if (isAdvanced) GameConstants.ADVANCED_TRAINING_COST_COINS
                       else GameConstants.BASIC_TRAINING_COST_COINS

        if (!userRepository.deductCoins(coinCost)) return Result.InsufficientCoins

        if (isAdvanced) {
            val consumed = mutableListOf<Pair<com.doge.simulator.domain.model.ResourceType, Long>>()
            for ((type, amount) in GameConstants.ADVANCED_TRAINING_RESOURCE_COST) {
                if (!resourceRepository.consume(type, amount.toLong())) {
                    userRepository.addCoins(coinCost)
                    for ((refundType, refundAmount) in consumed) resourceRepository.add(refundType, refundAmount)
                    return Result.InsufficientResources
                }
                consumed.add(type to amount.toLong())
            }
        }

        val duration = if (isAdvanced) GameConstants.ADVANCED_TRAINING_DURATION_MS
                       else GameConstants.BASIC_TRAINING_DURATION_MS
        val trainingEndTime = System.currentTimeMillis() + duration
        val trainingType = if (isAdvanced) TrainingType.ADVANCED else TrainingType.BASIC

        astronautRepository.updateStatus(astronaut.id, AstronautStatus.TRAINING, trainingEndTime, trainingType)
        return Result.Success
    }
}
