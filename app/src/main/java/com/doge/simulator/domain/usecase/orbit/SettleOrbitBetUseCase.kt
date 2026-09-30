package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitBetSettlement
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject

// 매치가 끝난 뒤(승/패 확정 또는 이탈) 재화를 정산한다. 베팅액은 매치 시작 시 이미
// 차감됐으므로(FR-014), 패배/이탈 시에는 추가 차감 없이 손실만 확정한다(FR-016).
class SettleOrbitBetUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    // 정상적으로 승패가 갈려 매치가 종료된 경우
    suspend fun settleFinished(matchState: OrbitMatchState): OrbitBetSettlement? {
        val bet = matchState.bet ?: return null
        val result = matchState.matchResult ?: return null
        val settlement = when (result) {
            MatchOutcome.WON -> {
                val reward = (bet.amount * bet.riskTier.winRewardMultiplier).toLong()
                userRepository.addCoins(reward)
                OrbitBetSettlement(MatchOutcome.WON, betAmount = bet.amount, netChange = reward)
            }
            MatchOutcome.LOST -> OrbitBetSettlement(MatchOutcome.LOST, betAmount = bet.amount, netChange = -bet.amount)
        }
        matchState.bet = bet.copy(settlement = settlement)
        return settlement
    }

    // 매치 도중 이탈: 결과 확정 없이 즉시 패배로 손실을 확정한다(FR-016)
    fun settleAbandoned(matchState: OrbitMatchState): OrbitBetSettlement? {
        val bet = matchState.bet ?: return null
        matchState.abandon()
        val settlement = OrbitBetSettlement(MatchOutcome.LOST, betAmount = bet.amount, netChange = -bet.amount)
        matchState.bet = bet.copy(settlement = settlement)
        return settlement
    }
}
