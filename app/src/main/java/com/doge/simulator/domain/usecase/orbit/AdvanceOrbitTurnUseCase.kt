package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.OrbitRoundState
import javax.inject.Inject

// 턴 시작 시 드로우를 시도한다. 덱이 비어 있으면 드로우 없이 즉시 라운드 종료 판정으로
// 넘긴다(FR-008). 성공하면 true(계속 진행 — 카드를 낼 차례), 실패하면 false(라운드가 이미
// 종료됨)를 반환한다.
class AdvanceOrbitTurnUseCase @Inject constructor(
    private val resolveOrbitRoundEndUseCase: ResolveOrbitRoundEndUseCase
) {
    operator fun invoke(round: OrbitRoundState): Boolean {
        if (round.isOver) return false
        val drew = round.drawForCurrentPlayer()
        if (!drew) {
            resolveOrbitRoundEndUseCase(round)
            return false
        }
        return true
    }
}
