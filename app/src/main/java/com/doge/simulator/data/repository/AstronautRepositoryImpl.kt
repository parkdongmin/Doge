package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.AstronautDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.repository.AstronautRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AstronautRepositoryImpl(
    private val dao: AstronautDao
) : AstronautRepository {

    override fun getAstronauts(): Flow<List<Astronaut>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun hire(astronaut: Astronaut) =
        dao.insert(astronaut.toEntity())

    override suspend fun updateStatus(id: String, status: AstronautStatus, trainingEndTime: Long?) =
        dao.updateStatus(id, status.name, trainingEndTime)

    override suspend fun completeTraining(id: String, newLevel: Int) =
        dao.completeTraining(id, newLevel)

    override suspend fun dismiss(id: String) =
        dao.delete(id)
}
