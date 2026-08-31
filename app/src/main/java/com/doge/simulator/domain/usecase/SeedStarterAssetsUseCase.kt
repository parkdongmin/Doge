package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 신규 게임 첫 진입 시 정찰선 1대 + 인턴 대원 1명 + 스타터 행성 1개를 무상 지급한다.
 *
 * 이게 없으면 신규 플레이어는 10,000코인만 들고 시작해서 (1) 첫 탐사를 보내려면 격납고
 * 구매·우주인 고용을 스스로 찾아야 하고 (2) 행성 발견 확률이 낮아 방치수익(이 게임의 핵심
 * 훅)을 한참 뒤에야 처음 본다. 스타터 행성으로 "가만히 둬도 돈이 벌린다"를 첫 진입부터
 * 체감시키고, 탐사는 행성을 "더" 늘리는 진행 목표가 된다.
 *
 * [UserRepository.initialize]가 true(= 방금 신규 유저 행을 만듦)를 반환할 때만 호출된다.
 * 방어적으로 각 자산이 이미 있으면 건너뛴다(스냅샷 복원 실패 후 재시드 등 예외 경로 대비).
 */
class SeedStarterAssetsUseCase @Inject constructor(
    private val spaceshipRepository: SpaceshipRepository,
    private val astronautRepository: AstronautRepository,
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val generatePlanetsUseCase: GeneratePlanetsUseCase
) {
    suspend operator fun invoke() {
        if (spaceshipRepository.getSpaceships().first().isEmpty()) {
            spaceshipRepository.buy(
                Spaceship(
                    name = "정찰선 MK-1",
                    grade = 1,
                    crewCapacity = GameConstants.SCOUT_CREW_BASE,
                    speed = GameConstants.SCOUT_SPEED_BASE,
                    cargo = GameConstants.SCOUT_CARGO_BASE,
                    successRate = GameConstants.SCOUT_SUCCESS_RATE_BASE
                )
            )
        }
        if (astronautRepository.getAstronauts().first().isEmpty()) {
            astronautRepository.hire(
                Astronaut(
                    name = "박 탐석",
                    specialty = AstronautSpecialty.MINERAL,
                    grade = AstronautGrade.INTERN,
                    proficiency = 20
                )
            )
        }
        if (planetRepository.getOwnedPlanets().first().isEmpty()) {
            val starter = generatePlanetsUseCase.starterPlanet()
            planetRepository.buyPlanet(starter)
            userRepository.recordVariantDiscovery(starter.variantId)
        }
    }
}
