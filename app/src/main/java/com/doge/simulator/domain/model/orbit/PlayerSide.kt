package com.doge.simulator.domain.model.orbit

enum class PlayerSide {
    PLAYER, B01;

    fun opponent(): PlayerSide = if (this == PLAYER) B01 else PLAYER
}
