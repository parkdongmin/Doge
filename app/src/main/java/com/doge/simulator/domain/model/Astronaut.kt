package com.doge.simulator.domain.model

import java.util.UUID

data class Astronaut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val specialty: AstronautSpecialty,
    val grade: AstronautGrade,
    val proficiency: Int,
    val status: AstronautStatus = AstronautStatus.IDLE,
    val trainingEndTime: Long? = null,
    val trainingType: TrainingType? = null,
    val hiredAt: Long = System.currentTimeMillis()
)
