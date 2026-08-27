package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.PlanetEventLogEntity
import com.doge.simulator.domain.model.PlanetEventLog

fun PlanetEventLogEntity.toDomain(): PlanetEventLog = PlanetEventLog(
    id = id,
    planetId = planetId,
    planetDisplayName = planetDisplayName,
    planetVariantCode = planetVariantCode,
    isPositive = isPositive,
    flavorText = flavorText,
    productionDeltaPerHour = productionDeltaPerHour,
    marketDelta = marketDelta,
    occurredAt = occurredAt
)

fun PlanetEventLog.toEntity(): PlanetEventLogEntity = PlanetEventLogEntity(
    id = id,
    planetId = planetId,
    planetDisplayName = planetDisplayName,
    planetVariantCode = planetVariantCode,
    isPositive = isPositive,
    flavorText = flavorText,
    productionDeltaPerHour = productionDeltaPerHour,
    marketDelta = marketDelta,
    occurredAt = occurredAt
)
