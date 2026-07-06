package com.doge.simulator.data.repository

import com.doge.simulator.domain.model.LeaderboardEntry
import com.doge.simulator.domain.repository.LeaderboardRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * 별도 leaderboard 컬렉션 없이 기존 users 컬렉션에 게임 자산 필드를 추가해
 * totalAsset 기준으로 순위를 산출한다.
 *
 * users/{uid} 문서에 추가되는 필드:
 *   - totalAsset  : Long  (코인 + 행성 시세 합산)
 *   - coins       : Long
 *   - planetCount : Int
 *   - assetUpdatedAt : Long
 */
class LeaderboardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LeaderboardRepository {

    private val users get() = firestore.collection("users")

    override suspend fun updateMyScore(
        uid: String,
        displayName: String,
        totalAsset: Long,
        coins: Long,
        planetCount: Int
    ) {
        // 기존 users 문서에 게임 통계 필드만 merge (인증 정보 덮어쓰지 않음)
        val data = mapOf(
            "totalAsset"     to totalAsset,
            "coins"          to coins,
            "planetCount"    to planetCount,
            "assetUpdatedAt" to System.currentTimeMillis()
        )
        users.document(uid).set(data, SetOptions.merge()).await()
    }

    override suspend fun getTopEntries(limit: Int): List<LeaderboardEntry> {
        return try {
            val snapshot = users
                .orderBy("totalAsset", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapIndexed { index, doc ->
                LeaderboardEntry(
                    uid         = doc.getString("uid") ?: doc.id,
                    displayName = doc.getString("displayName") ?: "익명",
                    totalAsset  = doc.getLong("totalAsset") ?: 0L,
                    coins       = doc.getLong("coins") ?: 0L,
                    planetCount = (doc.getLong("planetCount") ?: 0L).toInt(),
                    updatedAt   = doc.getLong("assetUpdatedAt") ?: 0L,
                    rank        = index + 1
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
