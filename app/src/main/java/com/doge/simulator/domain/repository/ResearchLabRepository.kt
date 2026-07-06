package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.model.ResearchLab
import kotlinx.coroutines.flow.Flow

interface ResearchLabRepository {
    fun get(): Flow<ResearchLab>
    suspend fun upgradeField(field: ResearchField, newLevel: Int)
    suspend fun initialize()
}
