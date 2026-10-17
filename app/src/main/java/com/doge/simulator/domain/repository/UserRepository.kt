package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.MissionProgress
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.TraitAcquisitionCounts
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

    // ── 2부 특성 행성 획득 기록 ───────────────────────────────────────────
    /** 발견한 행성을 얻은 순간(구매, 슬롯이 가득 찼을 때 "코인으로 받기") — 도감 등록 + 특성 행성이면 획득 기록 +1 */
    suspend fun recordPlanetAcquisition(planet: Planet)
    fun getTraitAcquisitionCounts(): Flow<TraitAcquisitionCounts>

    // ── 초보 임무 ─────────────────────────────────────────────────────
    fun getMissionProgress(): Flow<MissionProgress>
    suspend fun markOrbitPlayed()
    suspend fun markPlanetUpgradeTried()
    /** [step] 임무 보상을 지급하고 다음 임무로 넘긴다. 이미 넘어갔으면 false(아무것도 안 줌) */
    suspend fun claimMissionReward(step: Int, coins: Long, resources: Map<ResourceType, Long>): Boolean
}
