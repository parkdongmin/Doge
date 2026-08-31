package com.doge.simulator.domain.repository

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
}