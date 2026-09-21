package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow

// StartOrbitMatchUseCase/SettleOrbitBetUseCase 테스트에서 공유하는 최소 UserRepository 페이크.
class FakeOrbitUserRepository(initialCoins: Long = 0L) : UserRepository {
    private val coinsFlow = MutableStateFlow(initialCoins)
    val coins: Long get() = coinsFlow.value

    override fun getCoins() = coinsFlow
    override fun getDiscoveredVariantIds() = throw NotImplementedError()
    override suspend fun initialize(): Boolean = false
    override suspend fun addCoins(amount: Long) {
        coinsFlow.value += amount
    }

    override suspend fun deductCoins(amount: Long): Boolean {
        if (coinsFlow.value < amount) return false
        coinsFlow.value -= amount
        return true
    }

    override suspend fun deductCoinsClamped(amount: Long): Long {
        val actual = minOf(amount, coinsFlow.value)
        coinsFlow.value -= actual
        return actual
    }

    override suspend fun recordVariantDiscovery(variantId: String) {}
}
