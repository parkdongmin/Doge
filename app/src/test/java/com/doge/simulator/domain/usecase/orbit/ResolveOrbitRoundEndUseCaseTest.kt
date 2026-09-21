package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import org.junit.Assert.assertEquals
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
    fun `a draw round grants no signal and starts a fresh round`() {
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
