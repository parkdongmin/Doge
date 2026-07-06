package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.EventLogDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.EventLog
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.repository.EventLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventLogRepositoryImpl(
    private val dao: EventLogDao
) : EventLogRepository {

    override suspend fun insertLog(log: EventLog) {
        dao.insert(log.toEntity())
        val count = dao.count()
        if (count > GameConstants.MAX_EVENT_LOG_COUNT) {
            dao.deleteOldest(count - GameConstants.MAX_EVENT_LOG_COUNT)
        }
    }

    override fun getLogs(limit: Int, offset: Int): Flow<List<EventLog>> {
        return dao.getLogs(limit, offset).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLogCount(): Int = dao.count()
}
