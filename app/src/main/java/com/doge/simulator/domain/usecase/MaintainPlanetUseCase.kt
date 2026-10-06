package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.bankedProfitAt
import com.doge.simulator.domain.model.isBroken
import com.doge.simulator.domain.model.maintenanceCost
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// 고장(마이너스 생산) 행성을 정비해 정상 생산으로 되돌린다. 비용은 정상 생산량의 2시간치.
// 생산량이 바뀌므로 이벤트와 마찬가지로 바뀌기 전 몫을 먼저 적립하고, 수령·이벤트 롤과 같은 락 안에서
// DB 최신 상태를 다시 읽어 처리한다(그 사이 이벤트로 손해 배율이 바뀌었어도 지금 값 기준으로 정비)
class MaintainPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val collectProfitUseCase: CollectProfitUseCase
) {
    sealed class Result {
        data class Success(val cost: Long) : Result()
        object NotBroken : Result()
        object InsufficientCoins : Result()
    }

    suspend operator fun invoke(planetId: String): Result = collectProfitUseCase.withSettlementLock {
        val planet = planetRepository.getOwnedPlanets().first().firstOrNull { it.id == planetId }
        if (planet == null || !planet.isBroken) return@withSettlementLock Result.NotBroken

        val cost = planet.maintenanceCost
        if (!userRepository.deductCoins(cost)) return@withSettlementLock Result.InsufficientCoins

        val now = System.currentTimeMillis()
        if (!planetRepository.maintainPlanet(planet.id, planet.bankedProfitAt(now), now)) {
            userRepository.addCoins(cost)
            return@withSettlementLock Result.NotBroken
        }
        Result.Success(cost)
    }
}
