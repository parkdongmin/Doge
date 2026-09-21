package com.doge.simulator.domain.model.orbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class OrbitDeckTest {

    @Test
    fun `newShuffled contains exactly the 17-card composition`() {
        val deck = OrbitDeck.newShuffled(Random(42))
        val drawn = generateSequence { deck.draw() }.toList()
        assertEquals(OrbitCardType.TOTAL_CARD_COUNT, drawn.size)
        OrbitCardType.entries.forEach { type ->
            assertEquals(type.count, drawn.count { it.type == type })
        }
    }

    @Test
    fun `draw returns null once the deck is empty`() {
        val deck = OrbitDeck.forTest(listOf(OrbitCardType.SENSOR))
        assertEquals(OrbitCardType.SENSOR, deck.draw()?.type)
        assertNull(deck.draw())
    }

    @Test
    fun `markUsed records cards for internal AI tracking only`() {
        val deck = OrbitDeck.forTest(emptyList())
        val card = OrbitCard(OrbitCardType.PROBE)
        deck.markUsed(card)
        assertEquals(listOf(card), deck.usedCardsSnapshot())
    }

    @Test
    fun `remainingCount decreases as cards are drawn`() {
        val deck = OrbitDeck.forTest(listOf(OrbitCardType.SHIELD, OrbitCardType.EMP))
        assertEquals(2, deck.remainingCount)
        deck.draw()
        assertEquals(1, deck.remainingCount)
    }
}
