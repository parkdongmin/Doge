package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.TransactionRunner
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

// 슬롯이 가득 찼을 때 발견 행성을 "코인으로 받기" — 획득 기록·코인·"결과 처리됨"을 한 트랜잭션으로 묶는다.
// 따로 저장하면 그 사이 앱이 꺼졌을 때 결과 창이 다시 떠서 코인과 특성 행성 획득 기록이 두 번 들어갔다
class ClaimSlotFullCoinsUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val expeditionRepository: ExpeditionRepository,
    private val transactionRunner: TransactionRunner
) {
    // false면 이미 처리된 결과라 아무것도 주지 않았다
    // onResolved: BuyPlanetUseCase와 같은 이유로 "결과 처리됨" 저장 직전에 불린다
    suspend operator fun invoke(planet: Planet, coins: Long, expeditionId: String, onResolved: () -> Unit = {}): Boolean =
        transactionRunner.run {
            onResolved()
            if (!expeditionRepository.markResultHandled(expeditionId)) return@run false
            // 구매와 똑같이 "획득"으로 센다 — 도감 등록 + 특성 행성이면 획득 기록(T12~T15 해금 조건) +1
            userRepository.recordPlanetAcquisition(planet)
            userRepository.addCoins(coins)
            true
        }
}
