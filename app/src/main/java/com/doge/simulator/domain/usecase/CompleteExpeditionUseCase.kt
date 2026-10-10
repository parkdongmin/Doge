package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.expeditionDiscoveryChance
import com.doge.simulator.domain.model.expeditionResourceMultiplier
import com.doge.simulator.domain.model.expeditionSuccessChance
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.roundToLong
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

        val successChance = expeditionSuccessChance(spaceship, astronauts, expedition.category)
        val isSuccess = Random.nextFloat() < successChance

        val resources = mutableMapOf<ResourceType, Long>()
        var discoveredPlanetType: String? = null

        if (isSuccess) {
            // 자원 획득 (cargo 높을수록, 파견 인원이 많을수록, 매칭 전문가 숙련도 합이 높을수록,
            // 소요 시간이 길수록 더 많이). 코인 보상과 같은 방식(소요 시간 비례 + 티어당 완만한
            // 추가 배율)으로 스케일링해, 고티어(오래 걸림)가 저티어보다 시간당 자원 효율이
            // 떨어지는 역전이 생기지 않게 한다 — 기준 시간(10분)짜리 탐사가 배율 1로, 기본 랜덤 범위(1~5)
            val multiplier = expeditionResourceMultiplier(expedition.tier, spaceship, astronauts, expedition.category)
            val categoryResources = ResourceType.entries.filter { it.category == expedition.category }

            categoryResources.forEach { resourceType ->
                val baseAmount = Random.nextLong(1, 6)
                resources[resourceType] = (baseAmount * multiplier).roundToLong().coerceAtLeast(1L)
            }

            if (Random.nextFloat() < expeditionDiscoveryChance(lab, expedition.category)) {
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

    // 티어별 등급(rarity) 가중치(GameConstants.PLANET_RARITY_WEIGHTS)로 등급을 먼저 정하고,
    // 그 등급에 속한 행성 종류들 사이에서는 균등 분배 — 고티어라도 커먼/언커먼이 완전히 배제되지 않고,
    // 레전더리/에픽 비중은 캡이 있어 "무조건 최상급"이 되지 않게 함
    private fun rollPlanetType(tier: Int): String {
        val rarityWeights = GameConstants.PLANET_RARITY_WEIGHTS[tier]
            ?: GameConstants.PLANET_RARITY_WEIGHTS.getValue(GameConstants.PLANET_RARITY_WEIGHTS.keys.max())
        val typesByRarity = PlanetType.entries.groupBy { PlanetMetaDataTable.data.getValue(it).rarity }

        val weightedTypes = rarityWeights.flatMap { (rarity, rarityWeight) ->
            val typesInRarity = typesByRarity[rarity].orEmpty()
            typesInRarity.map { type -> type to rarityWeight / typesInRarity.size }
        }

        val totalWeight = weightedTypes.sumOf { it.second.toDouble() }
        var roll = Random.nextDouble() * totalWeight
        for ((type, weight) in weightedTypes) {
            roll -= weight
            if (roll <= 0) return type.name
        }
        return weightedTypes.last().first.name
    }
}
