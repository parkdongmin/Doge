package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.PlanetEventLog
import kotlinx.coroutines.flow.Flow

interface PlanetEventLogRepository {

    suspend fun addLog(log: PlanetEventLog)

    fun getRecentLogs(sinceMillis: Long): Flow<List<PlanetEventLog>>
}
