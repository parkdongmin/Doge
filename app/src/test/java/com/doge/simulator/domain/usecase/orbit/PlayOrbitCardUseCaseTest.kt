package com.doge.simulator.domain.usecase.orbit

import com.doge.simulator.domain.ai.B01Memory
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayOrbitCardUseCaseTest {

    private val useCase = PlayOrbitCardUseCase()

    private fun scoutDrone() = OrbitCard(OrbitCardType.SCOUT_DRONE)

    @Test
    fun `scout drone correct guess outs the opponent`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SCOUT_DRONE),
            b01Hand = listOf(OrbitCardType.CAPTAIN)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, scoutDrone(),
            OrbitCardEffectInput.ScoutGuess(8), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertEquals(PlayerSide.B01, result.summary.outSide)
        assertTrue(round.player(PlayerSide.B01).isOut)
        assertEquals(PlayerSide.PLAYER, round.winner)
    }

    @Test
    fun `scout drone wrong guess has no effect and passes the turn`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SCOUT_DRONE),
            b01Hand = listOf(OrbitCardType.CAPTAIN)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, scoutDrone(),
            OrbitCardEffectInput.ScoutGuess(5), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertNull(result.summary.outSide)
        assertFalse(round.player(PlayerSide.B01).isOut)
        assertEquals(PlayerSide.B01, round.currentTurn)
    }

    @Test
    fun `scout drone can never guess power 1`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SCOUT_DRONE),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, scoutDrone(),
            OrbitCardEffectInput.ScoutGuess(1), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertNull(result.summary.outSide)
        assertFalse(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `sensor reveals the opponent card to a human player as a one-off summary`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.EMP)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.SENSOR),
            b01Memory = B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertEquals(OrbitCardType.EMP, result.summary.revealedOpponentCard?.type)
    }

    @Test
    fun `sensor updates b01 memory when b01 uses it on the player`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN),
            b01Hand = listOf(OrbitCardType.SENSOR),
            turn = PlayerSide.B01
        )
        val memory = B01Memory()
        useCase(round, PlayerSide.B01, OrbitCard(OrbitCardType.SENSOR), b01Memory = memory)

        assertEquals(OrbitCardType.CAPTAIN, memory.knownPlayerCard?.type)
    }

    @Test
    fun `probe outs the lower power side`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.PROBE, OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE)
        )
        useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory())

        assertTrue(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `probe does nothing on a tie`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.PROBE, OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SENSOR)
        )
        useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory())

        assertFalse(round.player(PlayerSide.PLAYER).isOut)
        assertFalse(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `shield blocks a subsequent scout drone guess even if correct`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SHIELD),
            b01Hand = listOf(OrbitCardType.CAPTAIN, OrbitCardType.SCOUT_DRONE),
            turn = PlayerSide.PLAYER
        )
        useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.SHIELD), b01Memory = B01Memory())
        assertTrue(round.player(PlayerSide.PLAYER).shieldActive)

        // B01의 턴: 플레이어(SHIELD 보호 중)를 향해 CAPTAIN(8)을 정확히 지목해도 막혀야 한다.
        val result = useCase(
            round, PlayerSide.B01, OrbitCard(OrbitCardType.SCOUT_DRONE),
            OrbitCardEffectInput.ScoutGuess(8), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertNull(result.summary.outSide)
        assertFalse(round.player(PlayerSide.PLAYER).isOut)
    }

    @Test
    fun `a card that fizzles against shield is still consumed from hand`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SHIELD),
            b01Hand = listOf(OrbitCardType.CAPTAIN, OrbitCardType.PROBE),
            turn = PlayerSide.PLAYER
        )
        useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.SHIELD), b01Memory = B01Memory())
        useCase(round, PlayerSide.B01, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory())

        assertEquals(1, round.player(PlayerSide.B01).hand.size)
        assertFalse(round.player(PlayerSide.PLAYER).isOut)
        assertFalse(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `emp on opponent discards their card and redraws a new one`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.EMP),
            b01Hand = listOf(OrbitCardType.PROBE),
            remainingDeck = listOf(OrbitCardType.SHIELD)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.EMP),
            OrbitCardEffectInput.EmpTarget(PlayerSide.B01), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertNull(result.summary.outSide)
        assertEquals(OrbitCardType.SHIELD, round.player(PlayerSide.B01).hand.single().type)
        assertEquals(
            listOf(OrbitCardType.EMP, OrbitCardType.PROBE),
            round.deck.usedCardsSnapshot().map { it.type }
        )
    }

    @Test
    fun `emp forcing a captain discard outs that player`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.EMP),
            b01Hand = listOf(OrbitCardType.CAPTAIN),
            remainingDeck = listOf(OrbitCardType.PROBE)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.EMP),
            OrbitCardEffectInput.EmpTarget(PlayerSide.B01), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertEquals(PlayerSide.B01, result.summary.outSide)
        assertTrue(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `emp cannot be played when the deck is empty`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.EMP),
            b01Hand = listOf(OrbitCardType.CAPTAIN),
            remainingDeck = emptyList()
        )
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.EMP),
            OrbitCardEffectInput.EmpTarget(PlayerSide.B01), B01Memory()
        )
        assertEquals(PlayOrbitCardUseCase.Result.InvalidMove, result)
    }

    @Test
    fun `warp gate swaps hands and moving captain this way does not out anyone`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.WARP_GATE, OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.CAPTAIN)
        )
        useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.WARP_GATE), b01Memory = B01Memory())

        assertEquals(OrbitCardType.CAPTAIN, round.player(PlayerSide.PLAYER).hand.single().type)
        assertEquals(OrbitCardType.SENSOR, round.player(PlayerSide.B01).hand.single().type)
        assertFalse(round.player(PlayerSide.PLAYER).isOut)
        assertFalse(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `warp gate reveals b01's given-up card to its own memory`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.WARP_GATE, OrbitCardType.PROBE),
            turn = PlayerSide.B01
        )
        val memory = B01Memory()
        useCase(round, PlayerSide.B01, OrbitCard(OrbitCardType.WARP_GATE), b01Memory = memory)

        // B-01이 내준 카드(PROBE)가 그대로 플레이어 손에 들어갔으므로, B-01은 그 카드를 정확히 안다.
        assertEquals(OrbitCardType.PROBE, memory.knownPlayerCard?.type)
    }

    @Test
    fun `ai core must be played when held together with emp`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.AI_CORE, OrbitCardType.EMP),
            b01Hand = listOf(OrbitCardType.SENSOR),
            remainingDeck = listOf(OrbitCardType.PROBE)
        )
        val blocked = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.EMP),
            OrbitCardEffectInput.EmpTarget(PlayerSide.B01), B01Memory()
        )
        assertEquals(PlayOrbitCardUseCase.Result.InvalidMove, blocked)

        val allowed = useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.AI_CORE), b01Memory = B01Memory())
        assertTrue(allowed is PlayOrbitCardUseCase.Result.Applied)
    }

    @Test
    fun `ai core must be played when held together with warp gate`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.AI_CORE, OrbitCardType.WARP_GATE),
            b01Hand = listOf(OrbitCardType.SENSOR)
        )
        val blocked = useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.WARP_GATE), b01Memory = B01Memory())
        assertEquals(PlayOrbitCardUseCase.Result.InvalidMove, blocked)
    }

    @Test
    fun `playing captain directly outs the player who played it`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN),
            b01Hand = listOf(OrbitCardType.SENSOR)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.CAPTAIN), b01Memory = B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertEquals(PlayerSide.PLAYER, result.summary.outSide)
        assertTrue(round.player(PlayerSide.PLAYER).isOut)
        assertEquals(PlayerSide.B01, round.winner)
    }

    @Test
    fun `playing out of turn or a card not in hand is an invalid move`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.PROBE)
        )
        assertEquals(
            PlayOrbitCardUseCase.Result.InvalidMove,
            useCase(round, PlayerSide.B01, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory())
        )
        assertEquals(
            PlayOrbitCardUseCase.Result.InvalidMove,
            useCase(round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.CAPTAIN), b01Memory = B01Memory())
        )
    }
}
