package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.EventLog
import com.doge.simulator.domain.repository.EventLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventLogsUseCase @Inject constructor(
    private val repository: EventLogRepository
) {
    operator fun invoke(limit: Int = 50, offset: Int = 0): Flow<List<EventLog>> =
        repository.getLogs(limit, offset)
}
