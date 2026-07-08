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
}