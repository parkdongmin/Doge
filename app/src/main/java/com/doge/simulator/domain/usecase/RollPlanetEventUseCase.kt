package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetEventFlavor
import com.doge.simulator.domain.model.PlanetEventLog
import com.doge.simulator.domain.model.PlanetMetaDataTable
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
        val signedDelta = if (isBad) -delta else delta

        // 덮어쓰기가 아니라 누적 — 나쁜 이벤트가 연달아 겹치면 정말로 마이너스(실손해)까지 갈 수 있음.
        // 바닥·천장으로만 클램프해서 무한정 나빠지거나 좋아지지 않게 함
        val newMultiplier = (planet.productionMultiplier + signedDelta)
            .coerceIn(GameConstants.PLANET_EVENT_MULTIPLIER_FLOOR, GameConstants.PLANET_EVENT_MULTIPLIER_CEILING)

        // 시세는 이번 이벤트만큼 따로 누적하지 않고, 매번 "지금 누적된 생산 배율" 기준으로 다시 계산
        // — 생산 배율과 시세가 서로 다른 값으로 어긋나지 않음
        val deviation = newMultiplier - 1.0
        // 악재로 시세가 내려가되, 매입가+강화액을 다 깎아 매도가가 마이너스가 되는 건 막는다 —
        // 산 값보다 싸게 팔 순 있어도(손해), 파는데 코인을 더 내는 건 말이 안 됨
        val marketFloor = -(planet.buyPrice + planet.upgradeInvestment)
        val newMarketAdjustment = (
            deviation * planet.buyPrice +
                deviation * planet.upgradeInvestment * GameConstants.PLANET_EVENT_MARKET_UPGRADE_INVESTMENT_RATIO
            ).toLong().coerceAtLeast(marketFloor)

        planetRepository.updatePlanetEvent(planet.id, newMultiplier, newMarketAdjustment, now)

        // 소식 로그에는 "이번 이벤트 하나만으로" 얼마나 변했는지를 남긴다 — 상세화면의 생산 진행/
        // 시세 변동은 누적치를 보여주는 자리라 역할이 다름. 시세는 바닥에 걸리면 실제 반영폭이
        // 델타보다 작으므로, 로그에도 클램프 후 실제 변화분을 쓴다
        val meta = PlanetMetaDataTable.data[planet.type]
        val baseHourlyRate = planet.production * GameConstants.PLANET_PRODUCTION_SCALE *
            GameConstants.planetLevelMultiplier(planet.level) * 60.0
        val eventProductionDeltaPerHour = (signedDelta * baseHourlyRate).toLong()
        val eventMarketDelta = newMarketAdjustment - planet.marketAdjustment

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
            magnitude = delta
        )
    }
}

// 이벤트가 실제로 굴러갔을 때의 결과 요약 — 백그라운드 워커가 "큰 폭" 알림 여부를 판단하는 데 씀
data class PlanetEventRoll(
    val planetDisplayName: String,
    val isBad: Boolean,
    val magnitude: Double
)
