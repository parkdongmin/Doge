package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.RecruitmentCandidate
import com.doge.simulator.domain.repository.RecruitmentRepository
import javax.inject.Inject

/** 모집 풀 전체를 즉시 새로고침한다. 광고 시청 보상(현재는 스텁) 및 3시간 자동 새로고침에서 공통으로 사용. */
class RefreshRecruitmentPoolUseCase @Inject constructor(
    private val recruitmentRepository: RecruitmentRepository
) {
    suspend operator fun invoke() {
        val now = System.currentTimeMillis()
        repeat(GameConstants.RECRUITMENT_POOL_SIZE) { slotIndex ->
            recruitmentRepository.setCandidate(slotIndex, RecruitmentCandidate.random())
        }
        recruitmentRepository.setLastRefreshTime(now)
    }
}