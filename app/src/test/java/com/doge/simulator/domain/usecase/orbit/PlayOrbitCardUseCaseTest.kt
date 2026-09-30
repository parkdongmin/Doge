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

    // PROBE는 actor가 누구든 대칭적으로 동작해야 한다 — B-01이 PROBE를 내고 남는 카드가
    // SHIELD(4), 상대(플레이어)는 SCOUT_DRONE(1)만 들고 있으면 Power가 낮은 플레이어가
    // OUT돼야 한다(B-01 승리). 실기기에서 "결과가 반대로 보인다"는 리포트가 있어 확인차
    // 추가한 회귀 테스트 — 실제로는 정상이었고, 원인은 뷰모델이 라운드 종료 배너를 보여주기도
    // 전에 이미 다음 라운드를 시작해버려 화면에 다음 라운드 손패가 섞여 보였던 것이었다.
    @Test
    fun `probe is symmetric when b01 is the actor`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SCOUT_DRONE),
            b01Hand = listOf(OrbitCardType.PROBE, OrbitCardType.SHIELD),
            turn = PlayerSide.B01
        )
        val result = useCase(
            round, PlayerSide.B01, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertEquals(PlayerSide.PLAYER, result.summary.outSide)
        assertEquals(PlayerSide.B01, round.winner)
    }

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
        assertEquals("추측이 빗나갔어요", result.summary.noEffectNote)
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
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertFalse(round.player(PlayerSide.PLAYER).isOut)
        assertFalse(round.player(PlayerSide.B01).isOut)
        assertEquals("동점이라 아무 일도 없었어요", result.summary.noEffectNote)
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
        assertTrue(result.summary.blockedByShield)
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
        val result = useCase(
            round, PlayerSide.B01, OrbitCard(OrbitCardType.PROBE), b01Memory = B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertEquals(1, round.player(PlayerSide.B01).hand.size)
        assertTrue(result.summary.blockedByShield)
        assertFalse(round.player(PlayerSide.PLAYER).isOut)
        assertFalse(round.player(PlayerSide.B01).isOut)
    }

    @Test
    fun `shield does not report blocked for effects that do not target the shielded side`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SHIELD),
            b01Hand = listOf(OrbitCardType.SENSOR)
        )
        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.SHIELD), b01Memory = B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertFalse(result.summary.blockedByShield)
    }

    @Test
    fun `emp on self is not blocked by the actor's own shield`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.EMP, OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.PROBE),
            remainingDeck = listOf(OrbitCardType.CAPTAIN)
        )
        round.player(PlayerSide.PLAYER).shieldActive = true

        val result = useCase(
            round, PlayerSide.PLAYER, OrbitCard(OrbitCardType.EMP),
            OrbitCardEffectInput.EmpTarget(PlayerSide.PLAYER), B01Memory()
        ) as PlayOrbitCardUseCase.Result.Applied

        assertFalse(result.summary.blockedByShield)
        assertEquals(OrbitCardType.CAPTAIN, round.player(PlayerSide.PLAYER).hand.single().type)
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
        // OUT된 쪽은 새 카드를 받지 않는다 — 라운드 종료 때 공개되는 패가 방금 뽑은 엉뚱한 카드가
        // 아니라 버려진 CAPTAIN이어야 한다(요약의 discardedCard로 전달).
        assertTrue(round.player(PlayerSide.B01).hand.isEmpty())
        assertEquals(OrbitCardType.CAPTAIN, result.summary.discardedCard?.type)
        assertEquals(1, round.deck.remainingCount)
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
