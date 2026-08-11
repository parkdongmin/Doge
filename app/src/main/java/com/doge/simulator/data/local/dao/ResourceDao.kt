package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.ResourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourceDao {

    @Query("SELECT * FROM resource_table")
    fun getAll(): Flow<List<ResourceEntity>>

    @Query("SELECT amount FROM resource_table WHERE type = :type")
    suspend fun getAmount(type: String): Long?

    // 원자적 증가 — 행이 없으면 생성, 있으면 증감. 존재 확인 후 분기하는 방식은
    // 동시 호출 시 한쪽 증가분이 유실될 수 있어 INSERT ... ON CONFLICT로 한 번에 처리한다
    @Query("INSERT INTO resource_table (type, amount) VALUES (:type, :delta) ON CONFLICT(type) DO UPDATE SET amount = amount + :delta")
    suspend fun addAmountAtomic(type: String, delta: Long)

    // amount >= :amount 조건을 WHERE 절에 걸어 잔량 체크와 차감을 원자적으로 처리.
    // 반환값이 0이면 잔량 부족(또는 행 없음)으로 차감이 일어나지 않은 것
    @Query("UPDATE resource_table SET amount = amount - :amount WHERE type = :type AND amount >= :amount")
    suspend fun consumeAtomic(type: String, amount: Long): Int
}
