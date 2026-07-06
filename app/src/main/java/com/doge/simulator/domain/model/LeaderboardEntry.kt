package com.doge.simulator.domain.model

data class LeaderboardEntry(
    val uid: String = "",
    val displayName: String = "",
    /** 총 자산 = 보유 코인 + 행성 시세 합산 */
    val totalAsset: Long = 0L,
    val coins: Long = 0L,
    val planetCount: Int = 0,
    val updatedAt: Long = 0L,
    /** 클라이언트에서 계산되는 순위 (1-based) */
    val rank: Int = 0
)