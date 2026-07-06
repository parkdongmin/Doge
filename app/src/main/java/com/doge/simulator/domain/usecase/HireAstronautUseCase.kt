package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class HireAstronautUseCase @Inject constructor(
    private val astronautRepository: AstronautRepository,
    private val userRepository: UserRepository,
    private val researchLabRepository: ResearchLabRepository
) {
    sealed class Result {
        object Success : Result()
        object InsufficientCoins : Result()
        object MaxLimitReached : Result()
    }

    suspend operator fun invoke(specialty: AstronautSpecialty): Result {
        val lab = researchLabRepository.get().first()
        val astronauts = astronautRepository.getAstronauts().first()

        if (astronauts.size >= lab.maxAstronauts) return Result.MaxLimitReached

        val cost = GameConstants.ASTRONAUT_BASE_HIRE_COST +
                astronauts.size * GameConstants.ASTRONAUT_HIRE_COST_PER_EXISTING
        val coins = userRepository.getCoins().first()
        if (coins < cost) return Result.InsufficientCoins

        userRepository.addCoins(-cost)
        astronautRepository.hire(
            Astronaut(
                name = generateName(specialty),
                specialty = specialty
            )
        )
        return Result.Success
    }

    private fun generateName(specialty: AstronautSpecialty): String {
        val names = when (specialty) {
            AstronautSpecialty.MINERAL -> listOf("박 탐석", "김 광부", "이 지질", "최 채굴", "정 광석")
            AstronautSpecialty.PLANET -> listOf("유 우주", "강 탐사", "조 행성", "윤 항법", "장 천문")
            AstronautSpecialty.RUINS -> listOf("임 고고", "한 유물", "오 발굴", "서 역사", "신 탐험")
            AstronautSpecialty.ALIEN_CIVILIZATION -> listOf("권 외계", "황 연구", "안 분석", "송 접촉", "전 번역")
        }
        return names.random()
    }
}
