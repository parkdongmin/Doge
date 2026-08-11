package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.model.ResearchLab
import kotlinx.coroutines.flow.Flow

interface ResearchLabRepository {
    fun get(): Flow<ResearchLab>
    // 반환값이 false면 expectedLevel과 현재 레벨이 달라(중복 강화 시도 등) 반영되지 않았다는 뜻
    suspend fun upgradeField(field: ResearchField, expectedLevel: Int, newLevel: Int): Boolean
    suspend fun initialize()
}
