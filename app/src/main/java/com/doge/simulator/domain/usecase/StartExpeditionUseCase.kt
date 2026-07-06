package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StartExpeditionUseCase @Inject constructor(
    private val expeditionRepository: ExpeditionRepository,
    private val astronautRepository: AstronautRepository,
    private val spaceshipRepository: SpaceshipRepository,
    private val researchLabRepository: ResearchLabRepository,
    private val planetRepository: PlanetRepository
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

        // 티어 잠금 해제 조건 확인 (행성 보유 등급·수량 기반)
        val tierCondition = GameConstants.TIER_UNLOCK_CONDITIONS[tier]
        if (tierCondition != null) {
            val ownedPlanets = planetRepository.getOwnedPlanets().first()
            val qualifyingCount = ownedPlanets.count { planet ->
                val meta = PlanetMetaDataTable.data[planet.type]
                meta != null && meta.rarity.ordinal >= tierCondition.requiredRarity.ordinal
            }
            if (qualifyingCount < tierCondition.requiredCount)
                return Result.TierRequirementNotMet(tierCondition.label)
        }

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
        if (!allIdle) return Result.AstronautNotAvailable

        // 탐사 소요 시간 계산 (ship speed가 높을수록 단축)
        val baseMinutes = GameConstants.EXPEDITION_BASE_MINUTES[tier] ?: 20L
        val speedReduction = spaceship.speed / 200.0
        val durationMs = (baseMinutes * 60_000L * (1.0 - speedReduction)).toLong()
            .coerceAtLeast(TimeUnit.MINUTES.toMillis(5))

        val now = System.currentTimeMillis()
        val expedition = Expedition(
            category = category,
            tier = tier,
            astronautIds = astronautIds,
            spaceshipId = spaceshipId,
            startTime = now,
            endTime = now + durationMs
        )

        expeditionRepository.start(expedition)
        astronautIds.forEach { id ->
            astronautRepository.updateStatus(id, AstronautStatus.DEPLOYED)
        }

        return Result.Success(expedition)
    }
}
