package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.ExpeditionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpeditionDao {

    @Query("SELECT * FROM expedition_table WHERE status = 'IN_PROGRESS' ORDER BY startTime ASC")
    fun getActive(): Flow<List<ExpeditionEntity>>

    @Query("SELECT * FROM expedition_table ORDER BY startTime DESC")
    fun getAll(): Flow<List<ExpeditionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expedition: ExpeditionEntity)

    // status가 여전히 IN_PROGRESS일 때만 갱신 — 포그라운드 폴링과 백그라운드 워커가
    // 동시에 같은 탐사를 완료 처리하려 할 때 단 한 쪽만 성공하도록 막는 원자적 선점.
    // resultHandled = 0으로 표시해 어느 쪽이 선점했든 상관없이 결과 팝업이 뜨도록 한다
    @Query("""UPDATE expedition_table SET status = :status, resourcesResult = :resourcesResult,
        discoveredPlanetType = :discoveredPlanetType, coinsEarned = :coinsEarned, resultHandled = 0
        WHERE id = :id AND status = 'IN_PROGRESS'""")
    suspend fun complete(
        id: String,
        status: String,
        resourcesResult: String?,
        discoveredPlanetType: String?,
        coinsEarned: Long
    ): Int

    @Query("SELECT * FROM expedition_table WHERE resultHandled = 0 ORDER BY endTime ASC")
    fun getUnhandledResults(): Flow<List<ExpeditionEntity>>

    @Query("UPDATE expedition_table SET resultHandled = 1 WHERE id = :id")
    suspend fun markResultHandled(id: String)

    // 코인 단축: 아직 진행 중이고 끝나지 않은 탐사만 지금 끝나게 — 그사이 워커가 완료했거나 이미 끝났으면
    // 0을 돌려줘 호출부가 낸 코인을 돌려준다
    @Query("UPDATE expedition_table SET endTime = :now WHERE id = :id AND status = 'IN_PROGRESS' AND endTime > :now")
    suspend fun finishWaitNow(id: String, now: Long): Int

    // 광고 단축: 진행 중이고 아직 안 끝난 탐사만 byMs만큼 당긴다(지금보다 앞으로는 안 감). 광고를 보는 사이
    // 이미 끝났으면 0을 돌려줘 호출부가 하루 횟수를 차감하지 않는다
    @Query("UPDATE expedition_table SET endTime = MAX(:now, endTime - :byMs) WHERE id = :id AND status = 'IN_PROGRESS' AND endTime > :now")
    suspend fun shortenWaitBy(id: String, byMs: Long, now: Long): Int

    @Query("UPDATE expedition_table SET endTime = :endTime WHERE id = :id")
    suspend fun updateEndTime(id: String, endTime: Long)
}
