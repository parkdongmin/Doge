package com.doge.simulator.domain.model

data class PlanetMetaData(
    val type: PlanetType,
    val displayName: String,
    val description: String,

    val productionMin: Int,
    val productionMax: Int,

    val riskMin: Int,
    val riskMax: Int,

    val investmentMin: Int,
    val investmentMax: Int,

    val eventRateMin: Int,
    val eventRateMax: Int,

    val rarity: RarityTier,
    val basePrice: Int,
    val variants: List<PlanetVariant>   // 여기서 Wet-01 ~ Wet-30
)