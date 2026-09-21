package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.ads.OrbitDailyRewardGate
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

// 리워드 광고 시청이 RewardedAdResult.Earned로 확정된 뒤에만 ViewModel에서 호출해야 한다.
// 광고 표시 자체(Activity 의존)는 이 UseCase의 책임이 아니다(기존 RewardedAdManager 사용
// 관례와 동일 — AstronautViewModel.refreshPoolWithAd 참고). 이렇게 분리해두면 광고 로드
// 실패/도중 이탈(Dismissed/Failed/NotReady) 시 이 UseCase 자체가 호출되지 않아 자연히
// FR-026(미지급·횟수 미차감)이 만족된다.
class ClaimOrbitDailyAdRewardUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val orbitDailyRewardGate: OrbitDailyRewardGate
) {
    sealed class Result {
        data object Granted : Result()
        data object LimitReached : Result()
    }

    suspend operator fun invoke(): Result {
        if (!orbitDailyRewardGate.canClaimToday()) return Result.LimitReached
        userRepository.addCoins(GameConstants.ORBIT_DAILY_AD_REWARD_COINS)
        orbitDailyRewardGate.recordClaim()
        return Result.Granted
    }
}
