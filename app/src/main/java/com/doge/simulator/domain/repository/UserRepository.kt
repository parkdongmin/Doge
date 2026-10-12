package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.MissionProgress
import com.doge.simulator.domain.model.ResourceType
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCoins(): Flow<Long>
    fun getDiscoveredVariantIds(): Flow<Set<String>>
    /** 저장된 유저가 없어 신규 게임으로 초기화한 경우 true. (스타터 자산 지급 트리거) */
    suspend fun initialize(): Boolean
    suspend fun addCoins(amount: Long)
    suspend fun deductCoins(amount: Long): Boolean
    suspend fun deductCoinsClamped(amount: Long): Long
    suspend fun recordVariantDiscovery(variantId: String)

    // ── 초보 임무 ─────────────────────────────────────────────────────
    fun getMissionProgress(): Flow<MissionProgress>
    suspend fun markOrbitPlayed()
    suspend fun markPlanetUpgradeTried()
    /** [step] 임무 보상을 지급하고 다음 임무로 넘긴다. 이미 넘어갔으면 false(아무것도 안 줌) */
    suspend fun claimMissionReward(step: Int, coins: Long, resources: Map<ResourceType, Long>): Boolean
}
