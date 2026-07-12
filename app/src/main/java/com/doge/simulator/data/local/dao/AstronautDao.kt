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

    @Query("UPDATE astronaut_table SET status = :status, trainingEndTime = :trainingEndTime, trainingType = :trainingType WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, trainingEndTime: Long?, trainingType: String?)

    @Query("UPDATE astronaut_table SET proficiency = :proficiency, status = 'IDLE', trainingEndTime = NULL, trainingType = NULL WHERE id = :id")
    suspend fun completeTraining(id: String, proficiency: Int)

    @Query("DELETE FROM astronaut_table WHERE id = :id")
    suspend fun delete(id: String)
}