package com.doge.simulator.domain.ai

import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class B01OrbitAiTest {

    @Test
    fun `decision never depends on the player's actual hidden hand`() {
        val memory = B01Memory() // 확인된 정보 없음(모름)
        val roundA = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.SENSOR),
            turn = PlayerSide.B01
        )
        val roundB = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.PROBE), // 실제 손패만 다름
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.SENSOR),
            turn = PlayerSide.B01
        )

        val decisionA = B01OrbitAi.decide(roundA, memory, mistakeRate = 0f, random = Random(1))
        val decisionB = B01OrbitAi.decide(roundB, memory, mistakeRate = 0f, random = Random(1))

        assertEquals(decisionA.card.type, decisionB.card.type)
        assertEquals(decisionA.input, decisionB.input)
    }

    @Test
    fun `must play ai core when holding it together with emp`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.AI_CORE, OrbitCardType.EMP),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f)
        assertEquals(OrbitCardType.AI_CORE, decision.card.type)
    }

    @Test
    fun `must play ai core when holding it together with warp gate`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.WARP_GATE, OrbitCardType.AI_CORE),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f)
        assertEquals(OrbitCardType.AI_CORE, decision.card.type)
    }

    @Test
    fun `zero mistake rate always plays the higher-scored candidate`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SHIELD, OrbitCardType.CAPTAIN),
            turn = PlayerSide.B01
        )
        repeat(20) { seed ->
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f, random = Random(seed))
            assertEquals(OrbitCardType.SHIELD, decision.card.type)
        }
    }

    @Test
    fun `mistake rate of 1 picks a genuine alternative when it is not suicidal`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.SHIELD),
            turn = PlayerSide.B01
        )
        val best = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f, random = Random(0))
        val mistake = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(0))
        assertNotEquals(best.card.type, mistake.card.type)
    }

    // ── 판단 품질 (고위험 = 실수 0에서 이상한 수를 두지 않는지) ─────────────────────

    // 피드백: SCOUT(1)+PROBE(3)에서 PROBE를 내면 남은 Power 1로 비교해 거의 확실히 진다
    @Test
    fun `never probes while keeping a power 1 card`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.PROBE),
            remainingDeck = List(8) { OrbitCardType.SHIELD },
            turn = PlayerSide.B01
        )
        repeat(20) { seed ->
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f, random = Random(seed))
            assertEquals(OrbitCardType.SCOUT_DRONE, decision.card.type)
        }
    }

    // 피드백: WARP_GATE(6)+SCOUT(1)에서 교환하면 상대가 SCOUT를 받아 B-01이 가져간 자기 카드를 맞힌다
    @Test
    fun `never warps away a scout drone`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.WARP_GATE, OrbitCardType.SCOUT_DRONE),
            remainingDeck = List(8) { OrbitCardType.SHIELD },
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f, random = Random(3))
        assertEquals(OrbitCardType.SCOUT_DRONE, decision.card.type)
    }

    @Test
    fun `suicidal alternatives are not chosen even as mistakes`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.PROBE),
            remainingDeck = List(8) { OrbitCardType.SHIELD },
            turn = PlayerSide.B01
        )
        repeat(20) { seed ->
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(seed))
            assertEquals(OrbitCardType.SCOUT_DRONE, decision.card.type)
        }
    }

    @Test
    fun `emps the player when it knows they hold the captain`() {
        val memory = B01Memory().apply { reveal(OrbitCard(OrbitCardType.CAPTAIN)) }
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN),
            b01Hand = listOf(OrbitCardType.EMP, OrbitCardType.SHIELD),
            remainingDeck = List(5) { OrbitCardType.SENSOR },
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, memory, mistakeRate = 0f)
        assertEquals(OrbitCardType.EMP, decision.card.type)
        assertEquals(OrbitCardEffectInput.EmpTarget(PlayerSide.PLAYER), decision.input)
    }

    @Test
    fun `probes when holding the captain since nothing beats it`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.PROBE, OrbitCardType.CAPTAIN),
            remainingDeck = List(5) { OrbitCardType.SHIELD },
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f)
        assertEquals(OrbitCardType.PROBE, decision.card.type)
    }

    @Test
    fun `keeps the higher card when the deck is about to run out`() {
        // 덱이 비어 이번 턴 뒤 바로 Power 비교 — SENSOR(2)를 내고 AI_CORE(7)를 남겨야 한다
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.PROBE),
            b01Hand = listOf(OrbitCardType.SENSOR, OrbitCardType.AI_CORE),
            remainingDeck = emptyList(),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f)
        assertEquals(OrbitCardType.SENSOR, decision.card.type)
    }

    @Test
    fun `never emps itself while holding the captain`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.EMP, OrbitCardType.CAPTAIN),
            remainingDeck = List(5) { OrbitCardType.SHIELD },
            turn = PlayerSide.B01
        ).apply { player(PlayerSide.PLAYER).shieldActive = true }
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f)
        assertEquals(OrbitCardEffectInput.EmpTarget(PlayerSide.PLAYER), decision.input)
    }

    // CAPTAIN을 내면 즉시 자멸(OUT)이므로, 실수 메커니즘이라 해도 다른 카드가 하나라도
    // 있으면 CAPTAIN을 골라선 안 된다. "가끔 최적이 아닌 수를 두는" 실수와 "자살"은 다르다.
    @Test
    fun `never plays captain as a mistake when another card is available`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SHIELD, OrbitCardType.CAPTAIN),
            turn = PlayerSide.B01
        )
        repeat(20) { seed ->
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(seed))
            assertEquals(OrbitCardType.SHIELD, decision.card.type)
        }
    }

    @Test
    fun `scout drone guesses the known player card power when confident`() {
        val memory = B01Memory().apply { reveal(OrbitCard(OrbitCardType.CAPTAIN)) }
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.PROBE),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, memory, mistakeRate = 0f, random = Random(5))

        assertEquals(OrbitCardType.SCOUT_DRONE, decision.card.type)
        assertEquals(8, (decision.input as OrbitCardEffectInput.ScoutGuess).guessedPower)
    }

    // 회귀 테스트: AI가 고른 수는 항상 규칙상 낼 수 있어야 한다 — 예전엔 낼 수 없는 수를 고르면
    // InvalidMove로 턴이 안 넘어가 크래시로 이어졌고, EMP 2장 + 빈 덱에선 낼 수 있는 수가 아예 없었다
    @Test
    fun `chosen move is always legal even with two emps and an empty deck`() {
        repeat(20) { seed ->
            val round = OrbitRoundState.forTest(
                playerHand = listOf(OrbitCardType.SENSOR),
                b01Hand = listOf(OrbitCardType.EMP, OrbitCardType.EMP),
                remainingDeck = emptyList(),
                turn = PlayerSide.B01
            )
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(seed))
            val result = com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase()(
                round, PlayerSide.B01, decision.card, decision.input, B01Memory()
            )
            assertTrue(result is com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase.Result.Applied)
        }
    }

    // 회귀 테스트(실기기 크래시): 손에 같은 종류 카드 2장(예: SENSOR 2장)이 있으면 두
    // candidate가 값으로는 서로 구별되지 않는다("OrbitCard"는 type만으로 동등성을 따지는
    // data class). mistakeRate가 1이라 "최적이 아닌 카드"를 골라야 하는데, 인덱스가 아니라
    // 값으로 "다른 카드"를 찾으려 하면 NoSuchElementException이 터졌다.
    @Test
    fun `does not crash when choosing the alternative among identical duplicate cards`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.CAPTAIN),
            b01Hand = listOf(OrbitCardType.SENSOR, OrbitCardType.SENSOR),
            turn = PlayerSide.B01
        )

        repeat(20) { seed ->
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(seed))
            assertEquals(OrbitCardType.SENSOR, decision.card.type)
        }
    }

    @Test
    fun `scout drone never guesses power 1 even without prior information`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.CAPTAIN),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 0f, random = Random(2))

        assertEquals(OrbitCardType.SCOUT_DRONE, decision.card.type)
        val guess = (decision.input as OrbitCardEffectInput.ScoutGuess).guessedPower
        assertNotEquals(1, guess)
    }
}
