package com.doge.simulator.domain.model.orbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitRoundStateTest {

    // 회귀 테스트: PlayOrbitCardUseCase가 InvalidMove를 반환해 턴이 안 넘어간 상태에서
    // 호출자가 drawForCurrentPlayer()를 또 부르면(예전에 앱이 죽던 원인 — 손패가 3장이 되어
    // B01OrbitAi.decide()의 check(hand.size==2)가 터짐) 카드가 중복으로 뽑히면 안 된다.
    @Test
    fun `drawing twice in the same turn without ending it only adds one card`() {
        val round = OrbitRoundState.forTest(
            playerHand = emptyList(),
            b01Hand = emptyList(),
            remainingDeck = listOf(OrbitCardType.SENSOR, OrbitCardType.PROBE, OrbitCardType.SHIELD)
        )

        assertTrue(round.drawForCurrentPlayer())
        assertTrue(round.drawForCurrentPlayer()) // 같은 턴에 다시 호출해도 무시되어야 함

        assertEquals(1, round.player(PlayerSide.PLAYER).hand.size)
        assertEquals(2, round.deck.remainingCount)
    }

    @Test
    fun `ending the turn allows the next player to draw again`() {
        val round = OrbitRoundState.forTest(
            playerHand = emptyList(),
            b01Hand = emptyList(),
            remainingDeck = listOf(OrbitCardType.SENSOR, OrbitCardType.PROBE)
        )

        round.drawForCurrentPlayer()
        round.endTurnAndSwitchIfNotOver()
        round.drawForCurrentPlayer()

        assertEquals(1, round.player(PlayerSide.PLAYER).hand.size)
        assertEquals(1, round.player(PlayerSide.B01).hand.size)
        assertEquals(0, round.deck.remainingCount)
    }
}
