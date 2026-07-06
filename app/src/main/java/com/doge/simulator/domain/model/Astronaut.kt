package com.doge.simulator.domain.model

import java.util.UUID

data class Astronaut(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val specialty: AstronautSpecialty,
    val level: Int = 1,
    val status: AstronautStatus = AstronautStatus.IDLE,
    val trainingEndTime: Long? = null,
    val hiredAt: Long = System.currentTimeMillis()
)
