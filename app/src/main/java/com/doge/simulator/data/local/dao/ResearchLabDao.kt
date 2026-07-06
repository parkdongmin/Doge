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

    @Query("UPDATE research_lab_table SET explorationTechLevel = :level WHERE id = 1")
    suspend fun updateExplorationTech(level: Int)

    @Query("UPDATE research_lab_table SET celestialAnalysisLevel = :level WHERE id = 1")
    suspend fun updateCelestialAnalysis(level: Int)

    @Query("UPDATE research_lab_table SET hrLevel = :level WHERE id = 1")
    suspend fun updateHr(level: Int)

    @Query("UPDATE research_lab_table SET engineeringLevel = :level WHERE id = 1")
    suspend fun updateEngineering(level: Int)
}
