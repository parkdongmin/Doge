package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.TrainingType
import com.doge.simulator.domain.repository.AstronautRepository
import javax.inject.Inject

class CompleteTrainingUseCase @Inject constructor(
    private val astronautRepository: AstronautRepository
) {
    suspend operator fun invoke(astronaut: Astronaut) {
        val gain = when (astronaut.trainingType) {
            TrainingType.ADVANCED -> GameConstants.ADVANCED_TRAINING_PROFICIENCY_GAIN
            else -> GameConstants.BASIC_TRAINING_PROFICIENCY_GAIN
        }
        val newProficiency = (astronaut.proficiency + gain).coerceAtMost(astronaut.grade.proficiencyCap)
        astronautRepository.completeTraining(astronaut.id, newProficiency)
    }
}
