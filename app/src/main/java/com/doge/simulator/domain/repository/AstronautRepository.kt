package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.TrainingType
import kotlinx.coroutines.flow.Flow

interface AstronautRepository {
    fun getAstronauts(): Flow<List<Astronaut>>
    suspend fun hire(astronaut: Astronaut)
    suspend fun updateStatus(
        id: String,
        status: AstronautStatus,
        trainingEndTime: Long? = null,
        trainingType: TrainingType? = null
    )
    suspend fun completeTraining(id: String, newProficiency: Int)
    // 반환값이 false면 그사이 훈련이 이미 완료돼(status != TRAINING) 반영되지 않았다는 뜻
    suspend fun extendTraining(id: String, trainingEndTime: Long, trainingType: TrainingType?): Boolean
    suspend fun dismiss(id: String)
}