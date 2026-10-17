package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.Planet
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
        // 탐사 완료 시 확정한 발견 행성(없으면 null) — 종류와 함께 통째로 저장한다
        discoveredPlanet: Planet?,
        coinsEarned: Long = 0L
    ): Boolean

    // 완료됐지만 아직 결과 팝업을 보여주지 않은 탐사 목록 (누가 완료 처리를 선점했는지와 무관)
    fun getUnhandledResults(): Flow<List<Expedition>>
    // false면 이미 처리된 결과
    suspend fun markResultHandled(id: String): Boolean

    // 대기시간 스킵 광고 시청 시 종료 시각을 앞당기기 위함
    suspend fun updateEndTime(id: String, endTime: Long)

    // 코인 단축: 진행 중인 탐사를 지금 끝나게. 이미 끝났거나 완료 처리됐으면 false
    suspend fun finishWaitNow(id: String, now: Long): Boolean

    // 광고 단축: 진행 중인 탐사를 byMs만큼 당긴다. 이미 끝났거나 완료 처리됐으면 false
    suspend fun shortenWaitBy(id: String, byMs: Long, now: Long): Boolean
}
