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
    // 동시에 같은 탐사를 완료 처리하려 할 때 단 한 쪽만 성공하도록 막는 원자적 선점
    @Query("""UPDATE expedition_table SET status = :status, resourcesResult = :resourcesResult,
        discoveredPlanetType = :discoveredPlanetType WHERE id = :id AND status = 'IN_PROGRESS'""")
    suspend fun complete(id: String, status: String, resourcesResult: String?, discoveredPlanetType: String?): Int
}
