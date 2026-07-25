package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.ExpeditionDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.repository.ExpeditionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpeditionRepositoryImpl(
    private val dao: ExpeditionDao
) : ExpeditionRepository {

    override fun getActive(): Flow<List<Expedition>> =
        dao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getAll(): Flow<List<Expedition>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun start(expedition: Expedition) =
        dao.insert(expedition.toEntity())

    override suspend fun complete(
        id: String,
        status: ExpeditionStatus,
        resourcesResult: String?,
        discoveredPlanetType: String?,
        coinsEarned: Long
    ): Boolean =
        dao.complete(id, status.name, resourcesResult, discoveredPlanetType, coinsEarned) > 0

    override fun getUnhandledResults(): Flow<List<Expedition>> =
        dao.getUnhandledResults().map { list -> list.map { it.toDomain() } }

    override suspend fun markResultHandled(id: String) = dao.markResultHandled(id)

    override suspend fun updateEndTime(id: String, endTime: Long) = dao.updateEndTime(id, endTime)
}
