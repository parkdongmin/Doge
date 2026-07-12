package com.doge.simulator.domain.model

data class RecruitmentPool(
    val slots: List<RecruitmentCandidate?>,
    val lastRefreshTime: Long
) {
    val nextRefreshTime: Long
        get() = lastRefreshTime + GameConstants.RECRUITMENT_REFRESH_INTERVAL_MS
}