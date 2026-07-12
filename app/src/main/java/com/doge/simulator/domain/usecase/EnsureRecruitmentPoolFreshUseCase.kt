package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.repository.RecruitmentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** 마지막 새로고침으로부터 갱신 주기가 지났으면(또는 최초 실행이면) 모집 풀을 새로고침한다. */
class EnsureRecruitmentPoolFreshUseCase @Inject constructor(
    private val recruitmentRepository: RecruitmentRepository,
    private val refreshRecruitmentPoolUseCase: RefreshRecruitmentPoolUseCase
) {
    suspend operator fun invoke() {
        val lastRefresh = recruitmentRepository.getLastRefreshTime().first()
        val now = System.currentTimeMillis()
        if (lastRefresh == 0L || now - lastRefresh >= GameConstants.RECRUITMENT_REFRESH_INTERVAL_MS) {
            refreshRecruitmentPoolUseCase()
        }
    }
}