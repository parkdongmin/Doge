package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.EventLogEntity
import com.doge.simulator.domain.model.EventLog
import com.doge.simulator.domain.model.EventLogType

fun EventLogEntity.toDomain(): EventLog = EventLog(
    id = id,
    type = EventLogType.valueOf(type),
    planetId = planetId,
    planetType = planetType,
    planetDisplayName = planetDisplayName,
    description = description,
    valueBefore = valueBefore,
    valueAfter = valueAfter,
    coinDelta = coinDelta,
    occurredAt = occurredAt
)

fun EventLog.toEntity(): EventLogEntity = EventLogEntity(
    id = id,
    type = type.name,
    planetId = planetId,
    planetType = planetType,
    planetDisplayName = planetDisplayName,
    description = description,
    valueBefore = valueBefore,
    valueAfter = valueAfter,
    coinDelta = coinDelta,
    occurredAt = occurredAt
)
