package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.expeditionDurationMs
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.TransactionRunner
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StartExpeditionUseCase @Inject constructor(
    private val expeditionRepository: ExpeditionRepository,
    private val astronautRepository: AstronautRepository,
    private val spaceshipRepository: SpaceshipRepository,
    private val researchLabRepository: ResearchLabRepository,
    private val getTierProgress: GetTierProgressUseCase,
    private val transactionRunner: TransactionRunner
) {
    sealed class Result {
        data class Success(val expedition: Expedition) : Result()
        object CategoryLocked : Result()
        data class TierRequirementNotMet(val conditionLabel: String) : Result()
        object AstronautNotAvailable : Result()
        object SpaceshipNotFound : Result()
        object SpaceshipBusy : Result()
        object TeamTooLarge : Result()
    }

    suspend operator fun invoke(
        category: ExpeditionCategory,
        tier: Int,
        astronautIds: List<String>,
        spaceshipId: String
    ): Result {
        val lab = researchLabRepository.get().first()

        // 카테고리 해금 확인
        if (!lab.unlockedCategories().contains(category)) return Result.CategoryLocked

        // 티어 잠금 해제 조건 확인 (도감·획득 기록 기반, 사다리 방식 — TierProgress.firstLockedTier 참고)
        if (tier > GameConstants.MAX_TIER) return Result.TierRequirementNotMet("")
        val firstLocked = getTierProgress.once().firstLockedTier(maxTier = tier)
        if (firstLocked != null)
            return Result.TierRequirementNotMet(GameConstants.TIER_UNLOCK_CONDITIONS[firstLocked]!!.label)

        // 우주선 확인
        val spaceship = spaceshipRepository.getSpaceships().first()
            .firstOrNull { it.id == spaceshipId } ?: return Result.SpaceshipNotFound

        // 우주선 중복 파견 차단
        val activeSpaceshipIds = expeditionRepository.getActive().first()
            .map { it.spaceshipId }.toSet()
        if (spaceshipId in activeSpaceshipIds) return Result.SpaceshipBusy

        if (astronautIds.size > spaceship.crewCapacity) return Result.TeamTooLarge

        // 우주인 가용 상태 확인
        val astronauts = astronautRepository.getAstronauts().first()
            .filter { it.id in astronautIds }
        val allIdle = astronauts.all { it.status == AstronautStatus.IDLE }
        // 탐사 화면에서 골라둔 우주인이 그사이 방출됐으면 목록에서 빠져 있다
        if (!allIdle || astronauts.size != astronautIds.size) return Result.AstronautNotAvailable

        // 탐사 소요 시간 계산 (ship speed가 높을수록 단축)
        val durationMs = expeditionDurationMs(tier, spaceship)

        val now = System.currentTimeMillis()
        val expedition = Expedition(
            category = category,
            tier = tier,
            astronautIds = astronautIds,
            spaceshipId = spaceshipId,
            startTime = now,
            endTime = now + durationMs
        )

        // 탐사 행과 대원 DEPLOYED를 함께 — 중간에 끊기면 탐사 중인 대원이 대기로 보여 또 보내질 수 있다
        transactionRunner.run {
            expeditionRepository.start(expedition)
            astronautIds.forEach { id ->
                astronautRepository.updateStatus(id, AstronautStatus.DEPLOYED)
            }
        }

        return Result.Success(expedition)
    }
}
