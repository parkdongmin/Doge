package com.doge.simulator.domain.model

data class ObtainedResource(
    val id: String,
    val name: String,
    val rarity: RarityTier,
    val amount: Int
)