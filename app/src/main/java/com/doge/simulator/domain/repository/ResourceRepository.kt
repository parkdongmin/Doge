package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.model.ResourceType
import kotlinx.coroutines.flow.Flow

interface ResourceRepository {
    fun getAll(): Flow<List<Resource>>
    suspend fun getAmount(type: ResourceType): Long
    suspend fun add(type: ResourceType, amount: Long)
    suspend fun consume(type: ResourceType, amount: Long): Boolean
}
