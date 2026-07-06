package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.repository.AstronautRepository
import javax.inject.Inject

class CompleteTrainingUseCase @Inject constructor(
    private val astronautRepository: AstronautRepository
) {
    suspend operator fun invoke(astronaut: Astronaut) {
        astronautRepository.completeTraining(astronaut.id, astronaut.level + 1)
    }
}
