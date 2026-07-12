package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.AstronautEntity
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.TrainingType

fun AstronautEntity.toDomain() = Astronaut(
    id = id,
    name = name,
    specialty = AstronautSpecialty.valueOf(specialty),
    grade = AstronautGrade.valueOf(grade),
    proficiency = proficiency,
    status = AstronautStatus.valueOf(status),
    trainingEndTime = trainingEndTime,
    trainingType = trainingType?.let { TrainingType.valueOf(it) },
    hiredAt = hiredAt
)

fun Astronaut.toEntity() = AstronautEntity(
    id = id,
    name = name,
    specialty = specialty.name,
    grade = grade.name,
    proficiency = proficiency,
    status = status.name,
    trainingEndTime = trainingEndTime,
    trainingType = trainingType?.name,
    hiredAt = hiredAt
)