package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

class SellResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository,
    private val userRepository: UserRepository
) {
    sealed class Result {
        data class Success(val coinsEarned: Long) : Result()
        object InsufficientAmount : Result()
    }

    suspend operator fun invoke(type: ResourceType, amount: Long): Result {
        if (amount <= 0) return Result.InsufficientAmount
        val consumed = resourceRepository.consume(type, amount)
        if (!consumed) return Result.InsufficientAmount

        val unitPrice = GameConstants.RESOURCE_SELL_PRICE[type] ?: 10L
        val coinsEarned = unitPrice * amount
        userRepository.addCoins(coinsEarned)
        return Result.Success(coinsEarned)
    }
}