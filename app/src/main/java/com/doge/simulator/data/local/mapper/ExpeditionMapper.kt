package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.ExpeditionEntity
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionStatus

fun ExpeditionEntity.toDomain() = Expedition(
    id = id,
    category = ExpeditionCategory.valueOf(category),
    tier = tier,
    astronautIds = astronautIds.split(",").filter { it.isNotBlank() },
    spaceshipId = spaceshipId,
    startTime = startTime,
    endTime = endTime,
    status = ExpeditionStatus.valueOf(status),
    resourcesResult = resourcesResult,
    discoveredPlanetType = discoveredPlanetType,
    coinsEarned = coinsEarned
)

fun Expedition.toEntity() = ExpeditionEntity(
    id = id,
    category = category.name,
    tier = tier,
    astronautIds = astronautIds.joinToString(","),
    spaceshipId = spaceshipId,
    startTime = startTime,
    endTime = endTime,
    status = status.name,
    resourcesResult = resourcesResult,
    discoveredPlanetType = discoveredPlanetType,
    resultHandled = true,
    coinsEarned = coinsEarned
)
