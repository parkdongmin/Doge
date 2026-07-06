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

    val baseMaintenanceCostMin: Int,
    val baseMaintenanceCostMax: Int,
    val rarity: RarityTier,
    val basePrice: Int,
    val variants: List<PlanetVariant>,
    // 자원 종류 -> 분당 드롭 확률(%). CollectProfitUseCase에서 경과 시간에 비례해 정산.
    val resourceDrops: Map<ResourceType, Int> = emptyMap()
)