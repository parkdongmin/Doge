package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.ResearchLabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResearchLabDao {

    @Query("SELECT * FROM research_lab_table WHERE id = 1")
    fun get(): Flow<ResearchLabEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lab: ResearchLabEntity)

    @Query("INSERT OR IGNORE INTO `research_lab_table` VALUES (1, 1, 1, 1, 1)")
    suspend fun insertIfNotExists()

    // expectedLevel 가드 — 연타 등으로 같은 분야 강화가 거의 동시에 두 번 들어와도 두 번째
    // 호출은 이미 레벨이 바뀐 뒤라 반영되지 않는다. 반환값이 0이면 그사이 다른 강화가 먼저
    // 반영됐다는 뜻
    @Query("UPDATE research_lab_table SET explorationTechLevel = :level WHERE id = 1 AND explorationTechLevel = :expectedLevel")
    suspend fun updateExplorationTech(expectedLevel: Int, level: Int): Int

    @Query("UPDATE research_lab_table SET celestialAnalysisLevel = :level WHERE id = 1 AND celestialAnalysisLevel = :expectedLevel")
    suspend fun updateCelestialAnalysis(expectedLevel: Int, level: Int): Int

    @Query("UPDATE research_lab_table SET hrLevel = :level WHERE id = 1 AND hrLevel = :expectedLevel")
    suspend fun updateHr(expectedLevel: Int, level: Int): Int

    @Query("UPDATE research_lab_table SET engineeringLevel = :level WHERE id = 1 AND engineeringLevel = :expectedLevel")
    suspend fun updateEngineering(expectedLevel: Int, level: Int): Int
}
