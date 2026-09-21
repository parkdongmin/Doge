package com.doge.simulator.domain.model.orbit

data class OrbitBetSettlement(
    val outcome: MatchOutcome,
    val netChange: Long
)

data class OrbitBet(
    val amount: Long,
    val riskTier: OrbitRiskTier,
    val settlement: OrbitBetSettlement? = null
)
