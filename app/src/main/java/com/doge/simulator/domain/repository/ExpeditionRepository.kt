package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionStatus
import kotlinx.coroutines.flow.Flow

interface ExpeditionRepository {
    fun getActive(): Flow<List<Expedition>>
    fun getAll(): Flow<List<Expedition>>
    suspend fun start(expedition: Expedition)
    // 반환값: 실제로 이 호출이 IN_PROGRESS -> 완료 상태로 전환시켰는지 여부.
    // false면 다른 경로에서 이미 처리됐다는 뜻이므로 호출자는 보상 지급/보고서 생성을 건너뛰어야 한다
    suspend fun complete(
        id: String,
        status: ExpeditionStatus,
        resourcesResult: String?,
        discoveredPlanetType: String?,
        coinsEarned: Long = 0L
    ): Boolean

    // 완료됐지만 아직 결과 팝업을 보여주지 않은 탐사 목록 (누가 완료 처리를 선점했는지와 무관)
    fun getUnhandledResults(): Flow<List<Expedition>>
    suspend fun markResultHandled(id: String)
}
