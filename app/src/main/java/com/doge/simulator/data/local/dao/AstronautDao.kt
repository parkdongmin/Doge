package com.doge.simulator.data.local.dao

import androidx.room.*
import com.doge.simulator.data.local.entity.AstronautEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AstronautDao {

    @Query("SELECT * FROM astronaut_table ORDER BY hiredAt ASC")
    fun getAll(): Flow<List<AstronautEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(astronaut: AstronautEntity)

    @Query("UPDATE astronaut_table SET status = :status, trainingEndTime = :trainingEndTime, trainingType = :trainingType WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, trainingEndTime: Long?, trainingType: String?)

    // status = 'TRAINING' 가드 — 대기시간 단축 광고를 보는 동안 훈련이 이미 완료(IDLE로 전환)돼
    // 버렸다면, 이 갱신은 반영되지 않는다. 가드 없이 status까지 무조건 TRAINING으로 다시 쓰면
    // 이미 끝나서 다른 상태로 넘어갔을 수도 있는 우주인을 훈련 중으로 되살려버리게 된다.
    // 반환값이 0이면 그사이 훈련이 이미 완료됐다는 뜻
    @Query("UPDATE astronaut_table SET trainingEndTime = :trainingEndTime, trainingType = :trainingType WHERE id = :id AND status = 'TRAINING'")
    suspend fun extendTraining(id: String, trainingEndTime: Long, trainingType: String?): Int

    // status = 'TRAINING' 가드 — 포그라운드 폴링과 백그라운드 워커가 거의 동시에 완료 처리를
    // 시도해도 한쪽만 실제로 반영되게 해, proficiency 계산식이 바뀌어도 이중 적용되지 않는다
    @Query("UPDATE astronaut_table SET proficiency = :proficiency, status = 'IDLE', trainingEndTime = NULL, trainingType = NULL WHERE id = :id AND status = 'TRAINING'")
    suspend fun completeTraining(id: String, proficiency: Int)

    // 진행 중인 탐사 어디에도 없는데 DEPLOYED로 남은 대원을 대기로 되돌린다. 예전엔 탐사 완료 처리가
    // 트랜잭션이 아니어서, 탐사 행을 완료로 바꾼 직후 취소·종료되면 대원이 영영 "탐사 중"에 묶였다.
    // astronautIds는 쉼표로 이은 문자열이라 양끝에 쉼표를 붙여 정확히 한 칸만 맞춘다
    @Query("""UPDATE astronaut_table SET status = 'IDLE' WHERE status = 'DEPLOYED' AND NOT EXISTS (
        SELECT 1 FROM expedition_table e WHERE e.status = 'IN_PROGRESS'
        AND ',' || e.astronautIds || ',' LIKE '%,' || astronaut_table.id || ',%')""")
    suspend fun releaseOrphanedDeployed(): Int

    @Query("DELETE FROM astronaut_table WHERE id = :id")
    suspend fun delete(id: String)
}