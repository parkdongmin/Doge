package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.UserDao
import com.doge.simulator.data.local.entity.UserEntity
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

    override suspend fun initialize() {
        if (dao.getUserOnce() == null) {
            dao.upsertUser(UserEntity(coins = INITIAL_COINS))
        }
    }

    override suspend fun addCoins(amount: Long) {
        dao.addCoinsAtomic(amount)
    }

    override suspend fun deductCoins(amount: Long): Boolean =
        dao.deductCoinsAtomic(amount) > 0

    override suspend fun deductCoinsClamped(amount: Long): Long =
        dao.deductCoinsClamped(amount)

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