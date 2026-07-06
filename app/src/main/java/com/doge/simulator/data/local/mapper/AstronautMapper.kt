package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.AstronautEntity
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.AstronautStatus

fun AstronautEntity.toDomain() = Astronaut(
    id = id,
    name = name,
    specialty = AstronautSpecialty.valueOf(specialty),
    level = level,
    status = AstronautStatus.valueOf(status),
    trainingEndTime = trainingEndTime,
    hiredAt = hiredAt
)

fun Astronaut.toEntity() = AstronautEntity(
    id = id,
    name = name,
    specialty = specialty.name,
    level = level,
    status = status.name,
    trainingEndTime = trainingEndTime,
    hiredAt = hiredAt
)
