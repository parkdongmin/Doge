package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.SpaceshipDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.repository.SpaceshipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SpaceshipRepositoryImpl(
    private val dao: SpaceshipDao
) : SpaceshipRepository {

    override fun getSpaceships(): Flow<List<Spaceship>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun buy(spaceship: Spaceship) =
        dao.insert(spaceship.toEntity())

    override suspend fun upgrade(id: String, grade: Int, crewCapacity: Int, speed: Int, cargo: Int, successRate: Float) =
        dao.upgrade(id, grade, crewCapacity, speed, cargo, successRate)

    override suspend fun sell(id: String) =
        dao.delete(id)
}
