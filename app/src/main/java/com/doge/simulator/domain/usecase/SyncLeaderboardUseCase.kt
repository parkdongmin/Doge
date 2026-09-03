package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.marketValue
import com.doge.simulator.domain.repository.AuthRepository
import com.doge.simulator.domain.repository.LeaderboardRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 현재 플레이어의 총 자산(코인 + 행성 시세)을 계산해 Firestore 리더보드에 업데이트한다.
 * HomeScreen 진입 및 행성 구매/판매 후 호출.
 */
class SyncLeaderboardUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val planetRepository: PlanetRepository,
    private val leaderboardRepository: LeaderboardRepository
) {
    suspend operator fun invoke() {
        val user = authRepository.getCurrentUser() ?: return
        val coins = userRepository.getCoins().first()
        val planets = planetRepository.getOwnedPlanets().first()
        // 행성 가치는 매도가(SellPlanetUseCase)·자산 화면(AssetViewModel)과 동일하게
        // marketValue(buyPrice + upgradeInvestment + marketAdjustment)로 계산한다.
        val totalAsset = coins + planets.sumOf { it.marketValue }
        val displayName = user.displayName
            ?.takeIf { it.isNotBlank() }
            ?: "플레이어#${user.uid.take(6)}"

        leaderboardRepository.updateMyScore(
            uid = user.uid,
            displayName = displayName,
            totalAsset = totalAsset,
            coins = coins,
            planetCount = planets.size
        )
    }
}