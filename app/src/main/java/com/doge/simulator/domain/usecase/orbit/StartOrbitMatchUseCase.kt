package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.OrbitBet
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

// 베팅 화면에서 GAME START를 눌렀을 때 호출한다. 잔고 확인 → 즉시 차감(FR-014) → 매치 생성 순.
class StartOrbitMatchUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    sealed class Result {
        data class Success(val matchState: OrbitMatchState) : Result()
        data object InsufficientCoins : Result()
    }

    suspend operator fun invoke(amount: Long, riskTier: OrbitRiskTier): Result {
        val deducted = userRepository.deductCoins(amount)
        if (!deducted) return Result.InsufficientCoins
        val bet = OrbitBet(amount = amount, riskTier = riskTier)
        return Result.Success(OrbitMatchState.start(bet = bet))
    }
}
