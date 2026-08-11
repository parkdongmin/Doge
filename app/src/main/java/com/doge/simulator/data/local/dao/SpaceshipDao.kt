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

    // grade = :expectedGrade 가드 — 연타 등으로 같은 우주선에 대한 강화가 거의 동시에 두 번
    // 들어와도, 두 번째 호출은 이미 등급이 바뀐 뒤라 이 조건에 걸려 반영되지 않는다.
    // 반환값이 0이면 그사이 다른 강화가 먼저 반영됐다는 뜻
    @Query("UPDATE spaceship_table SET grade = :grade, crewCapacity = :crewCapacity, speed = :speed, cargo = :cargo, successRate = :successRate WHERE id = :id AND grade = :expectedGrade")
    suspend fun upgrade(id: String, expectedGrade: Int, grade: Int, crewCapacity: Int, speed: Int, cargo: Int, successRate: Float): Int

    @Query("DELETE FROM spaceship_table WHERE id = :id")
    suspend fun delete(id: String)
}
