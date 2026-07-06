package com.doge.simulator.data.local.mapper

import com.doge.simulator.data.local.entity.ResourceEntity
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.model.ResourceType

fun ResourceEntity.toDomain() = Resource(
    type = ResourceType.valueOf(type),
    amount = amount
)

fun Resource.toEntity() = ResourceEntity(
    type = type.name,
    amount = amount
)
