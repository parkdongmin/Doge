package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.SpaceshipEntity
import com.doge.simulator.domain.model.Spaceship

fun SpaceshipEntity.toDomain() = Spaceship(
    id = id,
    name = name,
    grade = grade,
    crewCapacity = crewCapacity,
    speed = speed,
    cargo = cargo,
    successRate = successRate,
    purchasedAt = purchasedAt
)

fun Spaceship.toEntity() = SpaceshipEntity(
    id = id,
    name = name,
    grade = grade,
    crewCapacity = crewCapacity,
    speed = speed,
    cargo = cargo,
    successRate = successRate,
    purchasedAt = purchasedAt
)
