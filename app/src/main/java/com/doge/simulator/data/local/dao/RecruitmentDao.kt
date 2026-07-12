package com.doge.simulator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doge.simulator.data.local.entity.RecruitmentCandidateEntity
import com.doge.simulator.data.local.entity.RecruitmentMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecruitmentDao {

    @Query("SELECT * FROM recruitment_candidate_table ORDER BY slotIndex ASC")
    fun getCandidates(): Flow<List<RecruitmentCandidateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCandidate(candidate: RecruitmentCandidateEntity)

    @Query("DELETE FROM recruitment_candidate_table WHERE slotIndex = :slotIndex")
    suspend fun deleteCandidate(slotIndex: Int)

    @Query("DELETE FROM recruitment_candidate_table")
    suspend fun clearCandidates()

    @Query("SELECT * FROM recruitment_meta_table WHERE id = 1")
    fun getMeta(): Flow<RecruitmentMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMeta(meta: RecruitmentMetaEntity)
}