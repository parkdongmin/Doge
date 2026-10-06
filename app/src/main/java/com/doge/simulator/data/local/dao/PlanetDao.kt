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

    // 수령하면 적립분도 함께 비운다(적립분은 totalProfit에 이미 포함돼 지급됨)
    @Query("UPDATE planet_table SET totalProfit = :totalProfit, lastProfitTime = :lastProfitTime, bankedProfit = 0, bankedUntil = 0 WHERE id = :id")
    suspend fun updateProfit(id: String, totalProfit: Long, lastProfitTime: Long)

    @Query("UPDATE planet_table SET level = :level, upgradeInvestment = :upgradeInvestment WHERE id = :planetId")
    suspend fun upgradePlanet(planetId: String, level: Int, upgradeInvestment: Long)

    @Query("""UPDATE planet_table SET productionMultiplier = :productionMultiplier,
              marketAdjustment = :marketAdjustment, lastEventTime = :lastEventTime,
              bankedProfit = :bankedProfit, bankedUntil = :bankedUntil WHERE id = :planetId""")
    suspend fun updatePlanetEvent(
        planetId: String,
        productionMultiplier: Double,
        marketAdjustment: Long,
        lastEventTime: Long,
        bankedProfit: Double,
        bankedUntil: Long
    )
}