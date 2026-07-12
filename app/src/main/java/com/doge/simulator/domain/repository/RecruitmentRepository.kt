package com.doge.simulator.domain.repository

import com.doge.simulator.domain.model.RecruitmentCandidate
import kotlinx.coroutines.flow.Flow

interface RecruitmentRepository {
    /** 크기가 항상 RECRUITMENT_POOL_SIZE인 리스트. null = 빈 슬롯 */
    fun getCandidates(): Flow<List<RecruitmentCandidate?>>
    fun getLastRefreshTime(): Flow<Long>
    suspend fun setCandidate(slotIndex: Int, candidate: RecruitmentCandidate)
    suspend fun clearCandidate(slotIndex: Int)
    suspend fun setLastRefreshTime(time: Long)
}