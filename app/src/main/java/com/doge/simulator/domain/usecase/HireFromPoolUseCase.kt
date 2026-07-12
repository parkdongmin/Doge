package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.RecruitmentRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class HireFromPoolUseCase @Inject constructor(
    private val recruitmentRepository: RecruitmentRepository,
    private val astronautRepository: AstronautRepository,
    private val userRepository: UserRepository,
    private val researchLabRepository: ResearchLabRepository
) {
    sealed class Result {
        object Success : Result()
        object InsufficientCoins : Result()
        object MaxLimitReached : Result()
        object SlotEmpty : Result()
    }

    suspend operator fun invoke(slotIndex: Int): Result {
        val lab = researchLabRepository.get().first()
        val astronauts = astronautRepository.getAstronauts().first()
        if (astronauts.size >= lab.maxAstronauts) return Result.MaxLimitReached

        val candidate = recruitmentRepository.getCandidates().first().getOrNull(slotIndex)
            ?: return Result.SlotEmpty

        val cost = candidate.hireCost(astronauts.size)
        val coins = userRepository.getCoins().first()
        if (coins < cost) return Result.InsufficientCoins

        userRepository.addCoins(-cost)
        astronautRepository.hire(
            Astronaut(
                name = candidate.name,
                specialty = candidate.specialty,
                grade = candidate.grade,
                proficiency = candidate.proficiency
            )
        )
        recruitmentRepository.clearCandidate(slotIndex)
        return Result.Success
    }
}