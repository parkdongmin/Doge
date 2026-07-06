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

    override suspend fun upgradeField(field: ResearchField, newLevel: Int) {
        dao.insertIfNotExists()
        when (field) {
            ResearchField.EXPLORATION_TECH -> dao.updateExplorationTech(newLevel)
            ResearchField.CELESTIAL_ANALYSIS -> dao.updateCelestialAnalysis(newLevel)
            ResearchField.HR_MANAGEMENT -> dao.updateHr(newLevel)
            ResearchField.SPACE_ENGINEERING -> dao.updateEngineering(newLevel)
        }
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
