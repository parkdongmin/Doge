package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.AstronautDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.TrainingType
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

    override suspend fun updateStatus(
        id: String,
        status: AstronautStatus,
        trainingEndTime: Long?,
        trainingType: TrainingType?
    ) = dao.updateStatus(id, status.name, trainingEndTime, trainingType?.name)

    override suspend fun completeTraining(id: String, newProficiency: Int) =
        dao.completeTraining(id, newProficiency)

    override suspend fun extendTraining(id: String, trainingEndTime: Long, trainingType: TrainingType?): Boolean =
        dao.extendTraining(id, trainingEndTime, trainingType?.name) > 0

    override suspend fun dismiss(id: String) =
        dao.delete(id)
}