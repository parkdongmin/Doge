package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.PlanetEntity
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetType

fun PlanetEntity.toDomain(): Planet {
    return Planet(
        id = id,
        type = PlanetType.valueOf(type),
        production = production,
        risk = risk,
        investment = investment,
        eventRate = eventRate,
        buyPrice = buyPrice,
        acquireTime = acquireTime,
        currentValue = currentValue,
        level = level,
        totalProfit = totalProfit,
        lastProfitTime = lastProfitTime
    )
}

fun Planet.toEntity(): PlanetEntity {
    return PlanetEntity(
        id = id,
        type = type.name,
        production = production,
        risk = risk,
        investment = investment,
        eventRate = eventRate,
        buyPrice = buyPrice,
        acquireTime = acquireTime,
        currentValue = currentValue,
        level = level,
        totalProfit = totalProfit,
        lastProfitTime = lastProfitTime
    )
}