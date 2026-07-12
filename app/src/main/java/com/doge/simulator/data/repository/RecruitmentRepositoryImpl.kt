package com.doge.simulator.data.repository

import com.doge.simulator.data.local.dao.RecruitmentDao
import com.doge.simulator.data.local.mapper.toDomain
import com.doge.simulator.data.local.mapper.toEntity
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.RecruitmentCandidate
import com.doge.simulator.domain.repository.RecruitmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecruitmentRepositoryImpl(
    private val dao: RecruitmentDao
) : RecruitmentRepository {

    override fun getCandidates(): Flow<List<RecruitmentCandidate?>> =
        dao.getCandidates().map { entities ->
            val bySlot = entities.associateBy { it.slotIndex }
            (0 until GameConstants.RECRUITMENT_POOL_SIZE).map { slotIndex ->
                bySlot[slotIndex]?.toDomain()
            }
        }

    override fun getLastRefreshTime(): Flow<Long> =
        dao.getMeta().map { it?.lastRefreshTime ?: 0L }

    override suspend fun setCandidate(slotIndex: Int, candidate: RecruitmentCandidate) =
        dao.upsertCandidate(candidate.toEntity(slotIndex))

    override suspend fun clearCandidate(slotIndex: Int) =
        dao.deleteCandidate(slotIndex)

    override suspend fun setLastRefreshTime(time: Long) =
        dao.setMeta(com.doge.simulator.data.local.entity.RecruitmentMetaEntity(lastRefreshTime = time))
}