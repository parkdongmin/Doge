package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.ResourceDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResourceRepositoryImpl(
    private val dao: ResourceDao
) : ResourceRepository {

    override fun getAll(): Flow<List<Resource>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAmount(type: ResourceType): Long =
        dao.getAmount(type.name) ?: 0L

    override suspend fun add(type: ResourceType, amount: Long) {
        dao.addAmountAtomic(type.name, amount)
    }

    override suspend fun consume(type: ResourceType, amount: Long): Boolean =
        dao.consumeAtomic(type.name, amount) > 0
}
