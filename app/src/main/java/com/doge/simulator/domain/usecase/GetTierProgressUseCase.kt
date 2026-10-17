package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.TierProgress
import com.doge.simulator.domain.repository.StoryRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// 티어 해금 판정 입력(도감·1부 완주·특성 행성 획득 기록)을 한 곳에서 묶는다 — 탐사 화면의 배너·티어 칩과
// 파견 검증(StartExpeditionUseCase)이 같은 값으로 판정해야 "보이는 건 열렸는데 파견은 막힘"이 안 생긴다
class GetTierProgressUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val storyRepository: StoryRepository
) {
    operator fun invoke(): Flow<TierProgress> = combine(
        userRepository.getDiscoveredVariantIds(),
        storyRepository.getProgress(),
        userRepository.getTraitAcquisitionCounts()
    ) { variantIds, story, counts ->
        TierProgress(
            discoveredVariantIds = variantIds,
            part1Completed = story.part1Completed,
            traitPlanetsAcquired = counts.traitPlanets,
            highFitTier2Acquired = counts.highFitTier2,
            highFitTier3Acquired = counts.highFitTier3
        )
    }

    suspend fun once(): TierProgress = invoke().first()
}
