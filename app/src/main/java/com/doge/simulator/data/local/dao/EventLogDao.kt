package com.doge.simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.doge.simulator.data.local.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {

    @Insert
    suspend fun insert(log: EventLogEntity)

    @Query("SELECT * FROM event_log_table ORDER BY occurredAt DESC LIMIT :limit OFFSET :offset")
    fun getLogs(limit: Int, offset: Int): Flow<List<EventLogEntity>>

    @Query("SELECT COUNT(*) FROM event_log_table")
    suspend fun count(): Int

    @Query("DELETE FROM event_log_table WHERE id IN (SELECT id FROM event_log_table ORDER BY occurredAt ASC LIMIT :count)")
    suspend fun deleteOldest(count: Int)
}
