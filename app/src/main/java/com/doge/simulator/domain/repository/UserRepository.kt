package com.doge.simulator.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCoins(): Flow<Long>
    fun getDiscoveredVariantIds(): Flow<Set<String>>
    suspend fun initialize()
    suspend fun addCoins(amount: Long)
    suspend fun deductCoins(amount: Long): Boolean
    suspend fun recordVariantDiscovery(variantId: String)
}