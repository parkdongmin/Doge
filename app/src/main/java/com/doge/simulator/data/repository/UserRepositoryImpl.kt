package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.UserDao
import com.doge.simulator.data.local.entity.UserEntity
import com.doge.simulator.domain.model.MissionProgress
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepositoryImpl(
    private val dao: UserDao
) : UserRepository {

    companion object {
        private const val INITIAL_COINS = 10_000L
    }

    override fun getCoins(): Flow<Long> = dao.getUser().map { it?.coins ?: 0L }

    override fun getDiscoveredVariantIds(): Flow<Set<String>> =
        dao.getUser().map { entity ->
            entity?.discoveredVariantIds
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
        }

    override suspend fun initialize(): Boolean {
        if (dao.getUserOnce() == null) {
            // 새 게임만 초보 임무 대상(0번 임무부터) — 기존 게임·복원 세이브는 -1로 남는다
            dao.upsertUser(UserEntity(coins = INITIAL_COINS, missionStep = 0))
            return true
        }
        return false
    }

    override suspend fun addCoins(amount: Long) {
        dao.addCoinsAtomic(amount)
    }

    override suspend fun deductCoins(amount: Long): Boolean =
        dao.deductCoinsAtomic(amount) > 0

    override suspend fun deductCoinsClamped(amount: Long): Long =
        dao.deductCoinsClamped(amount)

    override fun getMissionProgress(): Flow<MissionProgress> =
        dao.getUser().map { entity ->
            MissionProgress(
                step = entity?.missionStep ?: -1,
                orbitPlayed = entity?.orbitPlayed ?: false,
                planetUpgradeTried = entity?.planetUpgradeTried ?: false
            )
        }

    override suspend fun markOrbitPlayed() = dao.markOrbitPlayed()

    override suspend fun markPlanetUpgradeTried() = dao.markPlanetUpgradeTried()

    override suspend fun claimMissionReward(step: Int, coins: Long, resources: Map<ResourceType, Long>): Boolean =
        dao.claimMissionReward(step, coins, resources.mapKeys { it.key.name })

    override suspend fun recordVariantDiscovery(variantId: String) {
        if (variantId.isBlank()) return
        val current = dao.getUserOnce() ?: return
        val existing = current.discoveredVariantIds
            .split(",").filter { it.isNotBlank() }.toMutableSet()
        if (existing.add(variantId)) {
            dao.updateDiscoveredVariantIds(existing.joinToString(","))
        }
    }
}