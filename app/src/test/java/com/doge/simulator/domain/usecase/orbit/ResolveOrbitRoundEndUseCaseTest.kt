package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class ResolveOrbitRoundEndUseCaseTest {

    private val useCase = ResolveOrbitRoundEndUseCase()

    @Test
    fun `higher last-hand power wins when deck is exhausted`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN), // power 8
            b01Hand = listOf(OrbitCardType.SENSOR)      // power 2
        )
        useCase(round)
        assertEquals(RoundEndReason.DECK_EXHAUSTED, round.endReason)
        assertEquals(PlayerSide.PLAYER, round.winner)
    }

    @Test
    fun `b01 wins when its last hand power is higher`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SCOUT_DRONE), // power 1
            b01Hand = listOf(OrbitCardType.WARP_GATE)       // power 6
        )
        useCase(round)
        assertEquals(PlayerSide.B01, round.winner)
    }

    @Test
    fun `tied last-hand power falls back to cards-used-this-round power sum`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SENSOR)
        )
        round.player(PlayerSide.PLAYER).cardsUsedThisRound.add(com.doge.simulator.domain.model.orbit.OrbitCard(OrbitCardType.CAPTAIN))
        round.player(PlayerSide.B01).cardsUsedThisRound.add(com.doge.simulator.domain.model.orbit.OrbitCard(OrbitCardType.SCOUT_DRONE))

        useCase(round)
        assertEquals(PlayerSide.PLAYER, round.winner)
    }

    @Test
    fun `tied power and tied used-card sum results in a draw with no winner`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SENSOR)
        )
        useCase(round)
        assertEquals(RoundEndReason.DRAW, round.endReason)
        assertNull(round.winner)
    }

    @Test
    fun `a draw round grants no signal and does not end the match`() {
        val match = OrbitMatchState.start(Random(1))
        val drawnRound = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SENSOR),
            turn = match.currentRound.firstPlayerOfRound
        )
        useCase(drawnRound)
        match.currentRound = drawnRound

        match.onRoundEnded()

        assertEquals(0, match.signals[PlayerSide.PLAYER])
        assertEquals(0, match.signals[PlayerSide.B01])
        assertNull(match.matchResult)
    }

    // 회귀 테스트: onRoundEnded()가 SIGNAL/매치종료 판정만 하고 곧바로 다음 라운드를 시작하지
    // 않아야 한다 — 그렇지 않으면 "라운드 종료" 확인 배너가 떠 있는 동안에도 화면에 이미 다음
    // 라운드의 새 덱/새 손패가 섞여 보인다(실기기 리포트: 방금 끝난 라운드 결과가 헷갈림).
    // 다음 라운드는 startNextRoundIfNotOver()를 명시적으로 호출해야만 시작된다.
    @Test
    fun `onRoundEnded does not advance to the next round by itself`() {
        val match = OrbitMatchState.start(Random(1))
        val endedRound = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE),
            turn = match.currentRound.firstPlayerOfRound
        )
        useCase(endedRound)
        match.currentRound = endedRound

        match.onRoundEnded()
        assertEquals(endedRound, match.currentRound) // 아직 그대로 — 새 라운드 시작 안 됨

        match.startNextRoundIfNotOver()
        assertNotEquals(endedRound, match.currentRound) // 이제서야 다음 라운드로 넘어감
        assertEquals(PlayerSide.B01, match.currentRound.firstPlayerOfRound) // 직전 라운드 패자(B01)가 선공
    }

    @Test
    fun `winning three rounds ends the match for the player`() {
        val match = OrbitMatchState.start(Random(1))
        repeat(3) {
            val round = OrbitRoundState.forTest(
                playerHand = listOf(OrbitCardType.CAPTAIN),
                b01Hand = listOf(OrbitCardType.SCOUT_DRONE),
                turn = match.currentRound.firstPlayerOfRound
            )
            useCase(round)
            match.currentRound = round
            match.onRoundEnded()
        }
        assertEquals(MatchOutcome.WON, match.matchResult)
        assertEquals(3, match.signals[PlayerSide.PLAYER])
    }
}
