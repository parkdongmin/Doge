package com.doge.simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.doge.simulator.data.local.entity.PlanetEventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanetEventLogDao {

    @Insert
    suspend fun insertLog(log: PlanetEventLogEntity)

    @Query("SELECT * FROM planet_event_log_table WHERE occurredAt >= :sinceMillis ORDER BY occurredAt DESC")
    fun getRecentLogs(sinceMillis: Long): Flow<List<PlanetEventLogEntity>>

    // 오래된 로그는 무한정 쌓이지 않도록 최신 N개만 남기고 정리
    @Query("""DELETE FROM planet_event_log_table WHERE id NOT IN
              (SELECT id FROM planet_event_log_table ORDER BY occurredAt DESC LIMIT :keepCount)""")
    suspend fun trimOldLogs(keepCount: Int)
}
