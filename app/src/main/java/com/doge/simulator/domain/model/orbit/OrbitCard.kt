package com.doge.simulator.domain.model.orbit

data class OrbitCard(val type: OrbitCardType) {
    val power: Int get() = type.power
}
