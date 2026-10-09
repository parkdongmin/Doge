package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.bankedProfitAt
import com.doge.simulator.domain.model.isBroken
import com.doge.simulator.domain.model.maintenancePlan
import com.doge.simulator.domain.model.maintenanceResourceCost
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.repository.ResourceRepository
import com.doge.simulator.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// 고장(마이너스 생산) 행성을 정비해 정상 생산으로 되돌린다. 비용은 정상 생산량의 90분치 코인 + 등급·레벨별 자원
// (모자란 자원은 코인으로 대체). 생산량이 바뀌므로 이벤트와 마찬가지로 바뀌기 전 몫을 먼저 적립하고,
// 수령·이벤트 롤과 같은 락 안에서 DB 최신 상태를 다시 읽어 처리한다(그 사이 이벤트로 손해 배율이
// 바뀌었어도 지금 값 기준으로 정비)
class MaintainPlanetUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val userRepository: UserRepository,
    private val resourceRepository: ResourceRepository,
    private val collectProfitUseCase: CollectProfitUseCase
) {
    sealed class Result {
        data class Success(val coins: Long, val resources: Map<ResourceType, Long>) : Result()
        object NotBroken : Result()
        object InsufficientCoins : Result()
        // 계산 직후 다른 경로(판매 등)로 자원이 줄어 차감에 실패 — 환불했으니 다시 시도하면 새 잔량 기준으로 계산된다
        object ResourcesChanged : Result()
    }

    suspend operator fun invoke(planetId: String): Result = collectProfitUseCase.withSettlementLock {
        val planet = planetRepository.getOwnedPlanets().first().firstOrNull { it.id == planetId }
        if (planet == null || !planet.isBroken) return@withSettlementLock Result.NotBroken

        val owned = planet.maintenanceResourceCost.keys.associateWith { resourceRepository.getAmount(it) }
        val plan = planet.maintenancePlan { owned[it] ?: 0L }

        if (!userRepository.deductCoins(plan.totalCoins)) return@withSettlementLock Result.InsufficientCoins

        val consumed = mutableListOf<Pair<ResourceType, Long>>()
        suspend fun refund() {
            userRepository.addCoins(plan.totalCoins)
            for ((type, amount) in consumed) resourceRepository.add(type, amount)
        }
        // 잔량은 위에서 읽었지만 락 밖(탐사 정산·판매 등)에서 그 사이 줄었을 수 있어 원자적 차감 결과로 다시 확인
        for ((type, amount) in plan.resourcesUsed) {
            if (!resourceRepository.consume(type, amount)) {
                refund()
                return@withSettlementLock Result.ResourcesChanged
            }
            consumed.add(type to amount)
        }

        val now = System.currentTimeMillis()
        if (!planetRepository.maintainPlanet(planet.id, planet.bankedProfitAt(now), now)) {
            refund()
            return@withSettlementLock Result.NotBroken
        }
        Result.Success(plan.totalCoins, plan.resourcesUsed)
    }
}
