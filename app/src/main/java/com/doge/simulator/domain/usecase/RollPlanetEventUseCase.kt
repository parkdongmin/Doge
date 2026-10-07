package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetEventFlavor
import com.doge.simulator.domain.model.PlanetEventLog
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.bankedProfitAt
import com.doge.simulator.domain.model.isBroken
import com.doge.simulator.domain.model.marketAdjustmentFor
import com.doge.simulator.domain.model.marketValue
import com.doge.simulator.domain.model.preciseProduction
import com.doge.simulator.domain.repository.PlanetEventLogRepository
import com.doge.simulator.domain.repository.PlanetRepository
import javax.inject.Inject
import kotlin.random.Random

// 행성 하나에 이벤트가 밀렸으면 1회 롤하고 DB·소식 로그에 반영한다. 원래 CollectProfitUseCase
// 안에 있던 로직인데, 백그라운드에서 앱을 안 열어도 이벤트가 굴러가야(큰 폭 이벤트 알림 발송)
// 해서 포그라운드 수집 경로(CollectProfitUseCase)와 백그라운드 워커(PlanetEventWorker)가
// 공유할 수 있도록 별도 유스케이스로 분리
class RollPlanetEventUseCase @Inject constructor(
    private val planetRepository: PlanetRepository,
    private val planetEventLogRepository: PlanetEventLogRepository
) {
    // 마지막 이벤트 이후 평균 간격(risk 기반)을 넘었으면 딱 1번만 롤. 며칠을 방치했어도 몰아서
    // 여러 번 굴리지 않고, 확인하는 순간 결과 하나만 나옴 — "돌아와보니 뭔가 하나 일어나 있었다"
    suspend operator fun invoke(planet: Planet, now: Long): PlanetEventRoll? {
        val elapsedHours = (now - planet.lastEventTime) / 3_600_000.0
        val intervalHours = GameConstants.planetEventIntervalHours(planet.risk)
        if (elapsedHours < intervalHours) return null

        val isBad = Random.nextInt(100) < planet.eventRate
        val delta = Random.nextDouble(GameConstants.PLANET_EVENT_DELTA_MIN, GameConstants.PLANET_EVENT_DELTA_MAX)
        val after = applyEvent(planet, isBad, delta)

        // 생산량을 바꾸기 전에, 지금까지 바뀌기 전 생산량으로 번 몫을 적립해 둔다 — 안 그러면 나중에
        // 수령할 때 방치 시간 전체가 새 생산량으로 소급 계산된다(악재면 이벤트 전 시간까지 손해)
        planetRepository.updatePlanetEvent(
            planet.id, after.productionMultiplier, after.marketAdjustment, now,
            bankedProfit = planet.bankedProfitAt(now),
            bankedUntil = now,
            lossMultiplier = after.lossMultiplier
        )

        // 소식 로그에는 "이번 이벤트 하나만으로" 얼마나 변했는지를 남긴다 — 상세화면의 생산 진행/
        // 시세 변동은 누적치를 보여주는 자리라 역할이 다름
        val meta = PlanetMetaDataTable.data[planet.type]
        val eventProductionDeltaPerHour = ((after.preciseProduction - planet.preciseProduction) * 60.0).toLong()
        val eventMarketDelta = after.marketValue - planet.marketValue

        planetEventLogRepository.addLog(
            PlanetEventLog(
                planetId = planet.id,
                planetDisplayName = meta?.displayName ?: planet.type.name,
                planetVariantCode = planet.variantId.substringAfterLast("-"),
                isPositive = !isBad,
                flavorText = PlanetEventFlavor.random(isBad),
                productionDeltaPerHour = eventProductionDeltaPerHour,
                marketDelta = eventMarketDelta,
                occurredAt = now
            )
        )

        return PlanetEventRoll(
            planetDisplayName = meta?.displayName ?: planet.type.name,
            isBad = isBad,
            magnitude = delta,
            brokeDown = !planet.isBroken && after.isBroken
        )
    }

    companion object {
        // 이벤트 하나가 행성 상태를 어떻게 바꾸는지(순수 계산 — DB·랜덤 없음).
        //  - 정상 + 호재: 생산 배율 +델타
        //  - 정상 + 작은 악재(델타 < 고장 기준): 생산 배율 −델타만
        //  - 정상 + 큰 악재: 생산 배율 −델타 후 고장(손해 배율 1.0에서 시작)
        //  - 고장 + 악재: 손해 배율 +델타 (손해 커짐)
        //  - 고장 + 호재: 손해 배율 −델타 (손해 줄어듦, 플러스 복귀는 정비로만)
        fun applyEvent(planet: Planet, isBad: Boolean, delta: Double): Planet {
            if (planet.isBroken) {
                val newLoss = (planet.lossMultiplier + if (isBad) delta else -delta)
                    .coerceIn(GameConstants.PLANET_LOSS_MULTIPLIER_MIN, GameConstants.PLANET_LOSS_MULTIPLIER_MAX)
                return planet.copy(lossMultiplier = newLoss)
            }
            val newMultiplier = (planet.productionMultiplier + if (isBad) -delta else delta)
                .coerceIn(GameConstants.PLANET_EVENT_MULTIPLIER_FLOOR, GameConstants.PLANET_EVENT_MULTIPLIER_CEILING)
            val breaks = isBad && delta >= GameConstants.PLANET_BREAKDOWN_DELTA_THRESHOLD
            return planet.copy(
                productionMultiplier = newMultiplier,
                // 시세는 매번 "지금 생산 배율" 기준으로 다시 계산 — 생산 배율과 시세가 어긋나지 않음
                marketAdjustment = planet.marketAdjustmentFor(newMultiplier),
                lossMultiplier = if (breaks) GameConstants.PLANET_LOSS_MULTIPLIER_START else 0.0
            )
        }
    }
}

// 이벤트가 실제로 굴러갔을 때의 결과 요약 — 백그라운드 워커가 알림 여부를 판단하는 데 씀
data class PlanetEventRoll(
    val planetDisplayName: String,
    val isBad: Boolean,
    val magnitude: Double,
    // 이번 이벤트로 정상 → 고장이 됐는지. 정비가 필요해졌으니 폭과 상관없이 알린다
    val brokeDown: Boolean
)
