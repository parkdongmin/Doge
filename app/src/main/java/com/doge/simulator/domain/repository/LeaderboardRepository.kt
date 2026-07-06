package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.LeaderboardEntry

interface LeaderboardRepository {
    /** 내 점수를 Firestore에 저장/갱신 */
    suspend fun updateMyScore(
        uid: String,
        displayName: String,
        totalAsset: Long,
        coins: Long,
        planetCount: Int
    )

    /** 총 자산 기준 상위 [limit]명 조회 */
    suspend fun getTopEntries(limit: Int = 50): List<LeaderboardEntry>
}