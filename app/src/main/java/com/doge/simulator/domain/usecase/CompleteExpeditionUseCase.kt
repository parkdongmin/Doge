package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class CompleteExpeditionUseCase @Inject constructor(
    private val expeditionRepository: ExpeditionRepository,
    private val astronautRepository: AstronautRepository,
    private val spaceshipRepository: SpaceshipRepository,
    private val resourceRepository: ResourceRepository,
    private val researchLabRepository: ResearchLabRepository,
    private val userRepository: UserRepository,
    private val generateReportUseCase: GenerateExpeditionReportUseCase,
    private val storyRepository: com.doge.simulator.domain.repository.StoryRepository
) {
    data class ExpeditionResult(
        val success: Boolean,
        val resources: Map<ResourceType, Long>,
        val discoveredPlanetType: String?,
        val coinsEarned: Long = 0L
    )

    suspend operator fun invoke(expedition: Expedition): ExpeditionResult {
        val lab = researchLabRepository.get().first()
        val astronauts = astronautRepository.getAstronauts().first()
            .filter { it.id in expedition.astronautIds }
        val spaceship = spaceshipRepository.getSpaceships().first()
            .firstOrNull { it.id == expedition.spaceshipId }

        // 성공률: 우주선 기본값 + 전문 분야 일치 보너스
        val shipBaseRate = spaceship?.successRate ?: GameConstants.SCOUT_SUCCESS_RATE_BASE
        val specialtyBonus = astronauts.count { it.specialty.relatedCategory == expedition.category } *
                GameConstants.SPECIALTY_MATCH_SUCCESS_BONUS
        val successChance = (shipBaseRate + specialtyBonus).coerceIn(0.1f, 0.95f)
        val isSuccess = Random.nextFloat() < successChance

        val resources = mutableMapOf<ResourceType, Long>()
        var discoveredPlanetType: String? = null

        if (isSuccess) {
            // 자원 획득 (cargo 높을수록 더 많이)
            val cargoMultiplier = 1.0 + ((spaceship?.cargo ?: 50) - 50) / 100.0
            val categoryResources = ResourceType.entries.filter { it.category == expedition.category }
            val hasSpecialtyMatch = astronauts.any { it.specialty.relatedCategory == expedition.category }

            categoryResources.forEach { resourceType ->
                val baseAmount = Random.nextLong(1, 6)
                val specialtyMultiplier = if (hasSpecialtyMatch)
                    1.0 + GameConstants.SPECIALTY_MATCH_RESOURCE_BONUS else 1.0
                val amount = (baseAmount * cargoMultiplier * specialtyMultiplier).toLong().coerceAtLeast(1L)
                resources[resourceType] = amount
            }

            // 행성 발견 확률
            val baseChance = GameConstants.PLANET_DISCOVERY_BASE_CHANCE +
                    if (expedition.category == ExpeditionCategory.PLANET)
                        GameConstants.PLANET_DISCOVERY_PLANET_CATEGORY_BONUS else 0f
            val celestialBonus = lab.celestialAnalysisLevel * GameConstants.PLANET_DISCOVERY_CELESTIAL_BONUS_PER_LEVEL
            val discoveryChance = (baseChance + celestialBonus).coerceIn(0f, 0.8f)

            if (Random.nextFloat() < discoveryChance) {
                discoveredPlanetType = rollPlanetType(expedition.tier)
            }
        } else {
            // 실패해도 시간 투자가 완전히 헛수고가 되지 않도록 소량의 위로 보상을 지급
            val categoryResources = ResourceType.entries.filter { it.category == expedition.category }
            categoryResources.shuffled().take((categoryResources.size / 2).coerceAtLeast(1)).forEach { resourceType ->
                resources[resourceType] = Random.nextLong(1, 3)
            }
        }

        // 탐사 성공 시 행성 발견 여부와 무관하게 지급되는 기본 코인 보상.
        // 초반에 코인을 다 쓰고 행성도 못 찾았을 때 완전히 무수입 상태가 되는 것을 막는 안전망
        val coinsEarned = if (isSuccess) GameConstants.expeditionSuccessCoinReward(expedition.tier) else 0L

        // 탐사 기록 완료 처리
        val resourcesStr = resources.entries
            .joinToString(",") { "${it.key.name}:${it.value}" }
            .takeIf { it.isNotBlank() }

        // status가 여전히 IN_PROGRESS일 때만 전환에 성공한다. 포그라운드 폴링과 백그라운드
        // 워커가 같은 탐사를 거의 동시에 처리하려 할 때, 둘 중 하나만 통과시켜 보상·보고서가
        // 중복 생성되는 것을 막는다
        val claimed = expeditionRepository.complete(
            id = expedition.id,
            status = if (isSuccess) ExpeditionStatus.COMPLETED else ExpeditionStatus.FAILED,
            resourcesResult = resourcesStr,
            discoveredPlanetType = discoveredPlanetType,
            coinsEarned = coinsEarned
        )
        if (!claimed) return ExpeditionResult(success = false, resources = emptyMap(), discoveredPlanetType = null)

        resources.forEach { (type, amount) -> resourceRepository.add(type, amount) }
        if (coinsEarned > 0) userRepository.addCoins(coinsEarned)

        // 우주인 IDLE 복구
        expedition.astronautIds.forEach { id ->
            astronautRepository.updateStatus(id, AstronautStatus.IDLE)
        }

        // 탐사 보고서 생성 및 저장
        val report = generateReportUseCase(expedition, isSuccess)
        storyRepository.saveReport(report)

        return ExpeditionResult(isSuccess, resources, discoveredPlanetType, coinsEarned)
    }

    private fun rollPlanetType(tier: Int): String {
        val tierTypes = when (tier) {
            1 -> listOf("TERRAN_WET", "TERRAN_DRY", "ISLANDS", "NO_ATMOSPHERE")
            2 -> listOf("GAS_GIANT_1", "GAS_GIANT_2", "ICE_WORLD", "LAVA_WORLD")
            3 -> listOf("ASTEROID", "ICE_WORLD", "LAVA_WORLD", "GAS_GIANT_1")
            4 -> listOf("BLACK_HOLE", "GALAXY", "LAVA_WORLD")
            else -> listOf("GALAXY", "STAR", "BLACK_HOLE")
        }
        return tierTypes.random()
    }
}
