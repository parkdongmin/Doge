package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import javax.inject.Inject

// 자기 턴에 드로우해야 하는데 덱이 비어 있을 때 호출한다(FR-008). 드로우를 시도하지 않고
// 즉시 라운드를 종료하며, "마지막 손패 Power → 라운드 사용 카드 Power 총합 → DRAW" 순서로
// 비교한다.
class ResolveOrbitRoundEndUseCase @Inject constructor() {
    operator fun invoke(round: OrbitRoundState) {
        if (round.isOver) return

        val player = round.player(PlayerSide.PLAYER)
        val b01 = round.player(PlayerSide.B01)
        val playerPower = player.hand.firstOrNull()?.power ?: -1
        val b01Power = b01.hand.firstOrNull()?.power ?: -1

        when {
            playerPower > b01Power -> round.finishDeckExhausted(winner = PlayerSide.PLAYER)
            b01Power > playerPower -> round.finishDeckExhausted(winner = PlayerSide.B01)
            else -> {
                val playerSum = player.cardsUsedPowerSum()
                val b01Sum = b01.cardsUsedPowerSum()
                when {
                    playerSum > b01Sum -> round.finishDeckExhausted(winner = PlayerSide.PLAYER)
                    b01Sum > playerSum -> round.finishDeckExhausted(winner = PlayerSide.B01)
                    else -> round.finishDraw()
                }
            }
        }
    }
}
