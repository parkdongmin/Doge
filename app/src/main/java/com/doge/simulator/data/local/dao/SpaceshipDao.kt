package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.SpaceshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceshipDao {

    @Query("SELECT * FROM spaceship_table ORDER BY purchasedAt ASC")
    fun getAll(): Flow<List<SpaceshipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spaceship: SpaceshipEntity)

    @Query("UPDATE spaceship_table SET grade = :grade, crewCapacity = :crewCapacity, speed = :speed, cargo = :cargo, successRate = :successRate WHERE id = :id")
    suspend fun upgrade(id: String, grade: Int, crewCapacity: Int, speed: Int, cargo: Int, successRate: Float)

    @Query("DELETE FROM spaceship_table WHERE id = :id")
    suspend fun delete(id: String)
}
