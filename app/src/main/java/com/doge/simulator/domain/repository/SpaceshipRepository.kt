package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Spaceship
import kotlinx.coroutines.flow.Flow

interface SpaceshipRepository {
    fun getSpaceships(): Flow<List<Spaceship>>
    suspend fun buy(spaceship: Spaceship)
    suspend fun upgrade(id: String, grade: Int, crewCapacity: Int, speed: Int, cargo: Int, successRate: Float)
    suspend fun sell(id: String)
}
