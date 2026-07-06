package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautStatus
import kotlinx.coroutines.flow.Flow

interface AstronautRepository {
    fun getAstronauts(): Flow<List<Astronaut>>
    suspend fun hire(astronaut: Astronaut)
    suspend fun updateStatus(id: String, status: AstronautStatus, trainingEndTime: Long? = null)
    suspend fun completeTraining(id: String, newLevel: Int)
    suspend fun dismiss(id: String)
}
