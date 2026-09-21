package com.doge.simulator.domain.model.orbit

class OrbitPlayerState {
    val hand: MutableList<OrbitCard> = mutableListOf()

    var isOut: Boolean = false
        private set

    var shieldActive: Boolean = false

    val cardsUsedThisRound: MutableList<OrbitCard> = mutableListOf()

    fun markOut() {
        isOut = true
    }

    fun cardsUsedPowerSum(): Int = cardsUsedThisRound.sumOf { it.power }
}
