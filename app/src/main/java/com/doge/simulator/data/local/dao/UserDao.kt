package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_table WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user_table WHERE id = 1")
    suspend fun getUserOnce(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    // 원자적 증감 — 읽고 계산한 뒤 다시 쓰는 방식은 백그라운드 워커와 포그라운드 호출이
    // 동시에 코인을 갱신할 때 한쪽이 유실될 수 있어, DB 레벨에서 증감을 직접 수행한다
    @Query("UPDATE user_table SET coins = coins + :amount WHERE id = 1")
    suspend fun addCoinsAtomic(amount: Long)

    // coins >= amount 조건을 WHERE 절에 걸어 잔액 체크와 차감을 원자적으로 처리.
    // 반환값이 0이면 잔액 부족으로 차감이 일어나지 않은 것
    @Query("UPDATE user_table SET coins = coins - :amount WHERE id = 1 AND coins >= :amount")
    suspend fun deductCoinsAtomic(amount: Long): Int

    @Query("UPDATE user_table SET discoveredVariantIds = :ids WHERE id = 1")
    suspend fun updateDiscoveredVariantIds(ids: String)

    // ── 초보 임무 ─────────────────────────────────────────────────────
    @Query("UPDATE user_table SET orbitPlayed = 1 WHERE id = 1")
    suspend fun markOrbitPlayed()

    @Query("UPDATE user_table SET planetUpgradeTried = 1 WHERE id = 1")
    suspend fun markPlanetUpgradeTried()

    // missionStep이 아직 :current일 때만 넘긴다 — 받기 연타·동시 호출에도 한 번만 성공(반환 0 = 이미 받음)
    @Query("UPDATE user_table SET missionStep = :current + 1 WHERE id = 1 AND missionStep = :current")
    suspend fun advanceMissionStep(current: Int): Int

    @Query("INSERT INTO resource_table (type, amount) VALUES (:type, :delta) ON CONFLICT(type) DO UPDATE SET amount = amount + :delta")
    suspend fun addResourceAtomic(type: String, delta: Long)

    // 임무 진행과 보상 지급을 한 트랜잭션으로 — 중간에 앱이 죽어도 "넘어갔는데 보상 없음"이 안 생긴다
    @Transaction
    suspend fun claimMissionReward(current: Int, coins: Long, resources: Map<String, Long>): Boolean {
        if (advanceMissionStep(current) == 0) return false
        if (coins > 0) addCoinsAtomic(coins)
        resources.forEach { (type, amount) -> addResourceAtomic(type, amount) }
        return true
    }

    // 페널티 등 "잔액이 부족해도 있는 만큼은 깎여야 하는" 차감용 — 실제로 깎인 금액을 반환한다.
    // 같은 트랜잭션 안에서 읽고 쓰므로 동시 호출과 겹쳐도 실제 잔액 이상으로 깎이지 않는다
    @Transaction
    suspend fun deductCoinsClamped(amount: Long): Long {
        val current = getUserOnce()?.coins ?: 0L
        val actual = minOf(amount, current)
        if (actual > 0) addCoinsAtomic(-actual)
        return actual
    }
}