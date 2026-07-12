package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.RecruitmentPool
import com.doge.simulator.domain.repository.RecruitmentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetRecruitmentPoolUseCase @Inject constructor(
    private val recruitmentRepository: RecruitmentRepository
) {
    operator fun invoke(): Flow<RecruitmentPool> =
        combine(
            recruitmentRepository.getCandidates(),
            recruitmentRepository.getLastRefreshTime()
        ) { candidates, lastRefreshTime ->
            RecruitmentPool(candidates, lastRefreshTime)
        }
}