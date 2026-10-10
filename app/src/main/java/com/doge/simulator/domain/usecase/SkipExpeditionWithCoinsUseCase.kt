package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.ResearchLab
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.model.coinSkipCost
import com.doge.simulator.domain.model.coinSkipUnitPerMinute
import com.doge.simulator.domain.model.normalProduction
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResearchLabRepository
import com.doge.simulator.domain.repository.SpaceshipRepository
import com.doge.simulator.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// 코인으로 탐사 대기를 없앤다 — 광고 단축(하루 횟수 제한)과 별개로, 인게임 수입으로 진행을 살 수 있게.
// 비용 식은 coinSkipUnitPerMinute/coinSkipCost(화면 표시와 같은 식)
class SkipExpeditionWithCoinsUseCase @Inject constructor(
    private val expeditionRepository: ExpeditionRepository,
    private val spaceshipRepository: SpaceshipRepository,
    private val astronautRepository: AstronautRepository,
    private val researchLabRepository: ResearchLabRepository,
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository
) {
    sealed class Result {
        data class Success(val cost: Long) : Result()
        object InsufficientCoins : Result()
        object AlreadyFinished : Result()
    }

    // 화면 표시용 분당 단가. 남은 시간은 화면이 매초 갱신하니 단가만 넘기고 비용은 coinSkipCost로 계산
    fun unitPerMinute(
        expedition: Expedition,
        spaceships: List<Spaceship>,
        astronauts: List<Astronaut>,
        lab: ResearchLab,
        planets: List<Planet>
    ): Double = coinSkipUnitPerMinute(
        expedition = expedition,
        spaceship = spaceships.firstOrNull { it.id == expedition.spaceshipId },
        astronauts = astronauts.filter { it.id in expedition.astronautIds },
        lab = lab,
        // 고장 중인 행성도 정상 생산량으로 센다 — 고장(마이너스 생산)은 일시적인데, 그대로 더하면 수입 합계가
        // 깎여 단축이 싸지는 이상한 일이 생겼다
        passivePerMinute = planets.sumOf { it.normalProduction }
    )

    // quotedCost: 사용자가 확인 창에서 본 금액. 그사이 남은 시간이 줄어 실제 비용이 더 낮으면 낮은 쪽을,
    // 행성 이벤트 등으로 단가가 올랐어도 본 금액보다 더 받지는 않는다
    suspend operator fun invoke(expeditionId: String, quotedCost: Long, now: Long = System.currentTimeMillis()): Result {
        val expedition = expeditionRepository.getActive().first().firstOrNull { it.id == expeditionId }
        if (expedition == null || expedition.status != ExpeditionStatus.IN_PROGRESS || expedition.endTime <= now) {
            return Result.AlreadyFinished
        }
        val unit = unitPerMinute(
            expedition,
            spaceshipRepository.getSpaceships().first(),
            astronautRepository.getAstronauts().first(),
            researchLabRepository.get().first(),
            planetRepository.getOwnedPlanets().first()
        )
        val cost = minOf(quotedCost, coinSkipCost(expedition.endTime - now, unit))
        if (!userRepository.deductCoins(cost)) return Result.InsufficientCoins
        // 코인을 먼저 뺀 뒤 조건부로 종료 — 그사이 워커가 완료 처리했으면 0건 갱신이라 환불
        if (!expeditionRepository.finishWaitNow(expedition.id, now)) {
            userRepository.addCoins(cost)
            return Result.AlreadyFinished
        }
        return Result.Success(cost)
    }
}
