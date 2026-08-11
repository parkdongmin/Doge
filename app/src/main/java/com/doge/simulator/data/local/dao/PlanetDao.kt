package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.PlanetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanetDao {

    @Query("SELECT * FROM planet_table")
    fun getOwnedPlanets(): Flow<List<PlanetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanet(planet: PlanetEntity)

    // 반환값이 0이면 이미 삭제된 행성(연타 등으로 중복 매도 시도)이라는 뜻
    @Query("DELETE FROM planet_table WHERE id = :planetId")
    suspend fun deletePlanet(planetId: String): Int

    @Query("UPDATE planet_table SET totalProfit = :totalProfit, lastProfitTime = :lastProfitTime WHERE id = :id")
    suspend fun updateProfit(id: String, totalProfit: Long, lastProfitTime: Long)

    @Query("UPDATE planet_table SET level = :level, upgradeInvestment = :upgradeInvestment WHERE id = :planetId")
    suspend fun upgradePlanet(planetId: String, level: Int, upgradeInvestment: Long)
}