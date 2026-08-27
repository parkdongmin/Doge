package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.PlanetEventLogDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.PlanetEventLog
import com.doge.simulator.domain.repository.PlanetEventLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanetEventLogRepositoryImpl(
    private val dao: PlanetEventLogDao
) : PlanetEventLogRepository {

    override suspend fun addLog(log: PlanetEventLog) {
        dao.insertLog(log.toEntity())
        dao.trimOldLogs(GameConstants.MAX_EVENT_LOG_COUNT)
    }

    override fun getRecentLogs(sinceMillis: Long): Flow<List<PlanetEventLog>> {
        return dao.getRecentLogs(sinceMillis).map { list -> list.map { it.toDomain() } }
    }
}
