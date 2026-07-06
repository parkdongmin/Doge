package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.AstronautEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AstronautDao {

    @Query("SELECT * FROM astronaut_table ORDER BY hiredAt ASC")
    fun getAll(): Flow<List<AstronautEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(astronaut: AstronautEntity)

    @Query("UPDATE astronaut_table SET status = :status, trainingEndTime = :trainingEndTime WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, trainingEndTime: Long?)

    @Query("UPDATE astronaut_table SET level = :level, status = 'IDLE', trainingEndTime = NULL WHERE id = :id")
    suspend fun completeTraining(id: String, level: Int)

    @Query("DELETE FROM astronaut_table WHERE id = :id")
    suspend fun delete(id: String)
}
