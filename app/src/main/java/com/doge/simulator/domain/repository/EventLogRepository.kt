package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.EventLog
import kotlinx.coroutines.flow.Flow

interface EventLogRepository {
    suspend fun insertLog(log: EventLog)
    fun getLogs(limit: Int = 50, offset: Int = 0): Flow<List<EventLog>>
    suspend fun getLogCount(): Int
}
