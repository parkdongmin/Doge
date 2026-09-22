package com.doge.simulator.domain.ai

import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitRoundState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun `mistake rate of 1 picks a genuine alternative, never captain`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SCOUT_DRONE, OrbitCardType.SHIELD),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(0))
        assertEquals(OrbitCardType.SCOUT_DRONE, decision.card.type)
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

    // 회귀 테스트: 예전에는 덱이 비어 실제로 낼 수 없는 EMP를 "실수"로라도 골라버려서
    // PlayOrbitCardUseCase가 InvalidMove를 반환 → 턴이 안 넘어가 무한 반복하다 손패가
    // 늘어나 check(hand.size==2)가 터지는 크래시로 이어졌다. 덱이 비어 있으면 mistakeRate가
    // 1이어도 EMP를 절대 고르면 안 된다.
    @Test
    fun `never chooses emp when the deck is empty even at maximum mistake rate`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.EMP, OrbitCardType.SHIELD),
            remainingDeck = emptyList(),
            turn = PlayerSide.B01
        )

        repeat(20) { seed ->
            val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(seed))
            assertNotEquals(OrbitCardType.EMP, decision.card.type)
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
