package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.BeginnerMissionStatus
import com.doge.simulator.domain.model.MissionGameState
import com.doge.simulator.domain.model.beginnerMissionStatus
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.StoryRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// 지금 보여줄 초보 임무와 진행도. 대상이 아니거나 다 끝냈으면 null
class GetBeginnerMissionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val expeditionRepository: ExpeditionRepository,
    private val storyRepository: StoryRepository,
    private val getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val getResearchLabUseCase: GetResearchLabUseCase,
    private val getAstronautsUseCase: GetAstronautsUseCase,
    private val getSpaceshipsUseCase: GetSpaceshipsUseCase
) {
    operator fun invoke(): Flow<BeginnerMissionStatus?> {
        val counts = combine(
            expeditionRepository.getAll().map { it.size },
            storyRepository.getReadCount(),
            getOwnedPlanetsUseCase().map { planets -> planets.maxOfOrNull { it.level } ?: 0 },
            getResearchLabUseCase().map { lab ->
                lab.explorationTechLevel + lab.celestialAnalysisLevel + lab.hrLevel + lab.engineeringLevel - 4
            },
            combine(getAstronautsUseCase(), getSpaceshipsUseCase()) { a, s -> a.size to s.size }
        ) { expeditions, reads, maxLevel, research, (astronauts, ships) ->
            MissionGameState(
                expeditionCount = expeditions,
                readReportCount = reads,
                maxPlanetLevel = maxLevel,
                researchUpgrades = research,
                astronautCount = astronauts,
                spaceshipCount = ships,
                orbitPlayed = false,
                planetUpgradeTried = false
            )
        }
        return combine(userRepository.getMissionProgress(), counts) { progress, state ->
            beginnerMissionStatus(
                progress,
                state.copy(orbitPlayed = progress.orbitPlayed, planetUpgradeTried = progress.planetUpgradeTried)
            )
        }.distinctUntilChanged()
    }
}

// 지금 임무의 보상을 받고 다음 임무로 넘긴다. 누르는 순간 조건을 다시 확인해(그새 대원을 해고하는 등)
// 미완료면 아무것도 안 하고, 받기 연타는 DB에서 한 번만 통과시킨다
class ClaimBeginnerMissionUseCase @Inject constructor(
    private val getBeginnerMission: GetBeginnerMissionUseCase,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): BeginnerMissionStatus? {
        val status = getBeginnerMission().first() ?: return null
        if (!status.isComplete) return null
        val claimed = userRepository.claimMissionReward(
            step = status.index,
            coins = status.mission.rewardCoins,
            resources = status.mission.rewardResources
        )
        return status.takeIf { claimed }
    }
}
