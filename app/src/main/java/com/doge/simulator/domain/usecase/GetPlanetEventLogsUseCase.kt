package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.PlanetEventLog
import com.doge.simulator.domain.repository.PlanetEventLogRepository
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetPlanetEventLogsUseCase @Inject constructor(
    private val repository: PlanetEventLogRepository
) {
    // "소식" 탭 — 최근 24시간 이내 발생한 행성 이벤트만
    operator fun invoke(): Flow<List<PlanetEventLog>> =
        repository.getRecentLogs(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24))
}
