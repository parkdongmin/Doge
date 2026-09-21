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
    fun `mistake rate of 1 always plays the alternative candidate`() {
        val round = OrbitRoundState.forTest(
            playerHand = listOf(OrbitCardType.SENSOR),
            b01Hand = listOf(OrbitCardType.SHIELD, OrbitCardType.CAPTAIN),
            turn = PlayerSide.B01
        )
        val decision = B01OrbitAi.decide(round, B01Memory(), mistakeRate = 1f, random = Random(0))
        assertEquals(OrbitCardType.CAPTAIN, decision.card.type)
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
