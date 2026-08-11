package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.ResearchLabDao
import com.doge.simulator.data.local.entity.ResearchLabEntity
import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.model.ResearchLab
import com.doge.simulator.domain.repository.ResearchLabRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ResearchLabRepositoryImpl(
    private val dao: ResearchLabDao
) : ResearchLabRepository {

    override fun get(): Flow<ResearchLab> = dao.get().map { entity ->
        entity?.let {
            ResearchLab(
                explorationTechLevel = it.explorationTechLevel,
                celestialAnalysisLevel = it.celestialAnalysisLevel,
                hrLevel = it.hrLevel,
                engineeringLevel = it.engineeringLevel
            )
        } ?: ResearchLab()
    }

    override suspend fun upgradeField(field: ResearchField, expectedLevel: Int, newLevel: Int): Boolean {
        dao.insertIfNotExists()
        val rows = when (field) {
            ResearchField.EXPLORATION_TECH -> dao.updateExplorationTech(expectedLevel, newLevel)
            ResearchField.CELESTIAL_ANALYSIS -> dao.updateCelestialAnalysis(expectedLevel, newLevel)
            ResearchField.HR_MANAGEMENT -> dao.updateHr(expectedLevel, newLevel)
            ResearchField.SPACE_ENGINEERING -> dao.updateEngineering(expectedLevel, newLevel)
        }
        return rows > 0
    }

    override suspend fun initialize() {
        dao.upsert(ResearchLabEntity(
            explorationTechLevel = 1,
            celestialAnalysisLevel = 1,
            hrLevel = 1,
            engineeringLevel = 1
        ))
    }
}
