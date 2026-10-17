package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.acquiredAt
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.TransactionRunner
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class BuyPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val researchLabRepository: ResearchLabRepository,
    private val expeditionRepository: ExpeditionRepository,
    private val transactionRunner: TransactionRunner
) {
    enum class Result { BOUGHT, FAILED, ALREADY_HANDLED }

    // expeditionId: 이 행성을 발견한 탐사 결과. 구매와 "결과 처리됨" 표시를 한 트랜잭션으로 묶는다 — 따로 저장하면
    // 그 사이 앱이 꺼졌을 때 결과 창이 다시 떠서 같은 행성을 또 사고 코인이 두 번 빠졌다
    // onResolved: "결과 처리됨"을 저장하기 직전에 불린다 — 화면이 결과 창을 먼저 닫아 둬야, 저장 후 다음 미처리 결과가
    // 이어서 뜬다(ExploreViewModel의 미처리 결과 관찰은 결과 창이 열려 있으면 건너뛴다)
    suspend operator fun invoke(planet: Planet, expeditionId: String, onResolved: () -> Unit = {}): Result {
        val lab = researchLabRepository.get().first()
        val ownedCount = planetRepository.getOwnedPlanets().first().size
        if (ownedCount >= lab.maxPlanetSlots) return Result.FAILED

        return try {
            transactionRunner.run {
                if (!userRepository.deductCoins(planet.buyPrice.toLong())) return@run Result.FAILED

                planetRepository.buyPlanet(planet.acquiredAt(System.currentTimeMillis()))
                // 도감 등록 + 특성 행성이면 획득 기록(T12~T15 해금 조건) +1 — 매도해도 줄지 않는다
                userRepository.recordPlanetAcquisition(planet)
                onResolved()
                // 이미 처리된 결과면 위 차감·구매까지 통째로 되돌린다
                if (!expeditionRepository.markResultHandled(expeditionId)) throw AlreadyHandled()
                Result.BOUGHT
            }
        } catch (e: AlreadyHandled) {
            Result.ALREADY_HANDLED
        }
    }

    private class AlreadyHandled : RuntimeException()
}
