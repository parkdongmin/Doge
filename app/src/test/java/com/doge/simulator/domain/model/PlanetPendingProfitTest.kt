package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

// 방치 중 이벤트로 생산량이 바뀌어도, 바뀌기 전 시간은 바뀌기 전 생산량으로 정산되는지 확인.
// production 40 × PLANET_PRODUCTION_SCALE 0.15 = 레벨 1·배율 1.0 기준 분당 6코인.
class PlanetPendingProfitTest {

    private val minute = 60_000L

    private fun planet(multiplier: Double = 1.0) = Planet(
        type = PlanetType.NO_ATMOSPHERE,
        production = 40, risk = 5, investment = 0, eventRate = 50, buyPrice = 1000,
        acquireTime = 0L, lastProfitTime = 0L, lastEventTime = 0L,
        productionMultiplier = multiplier
    )

    // RollPlanetEventUseCase가 하는 일: 적립 후 배율 교체
    private fun Planet.applyEvent(at: Long, newMultiplier: Double = productionMultiplier, loss: Double = lossMultiplier) =
        copy(bankedProfit = bankedProfitAt(at), bankedUntil = at, productionMultiplier = newMultiplier, lossMultiplier = loss)

    @Test
    fun `without events pending profit is rate times elapsed minutes`() {
        assertEquals(6.0 * 480, planet().pendingProfitAt(480 * minute), 1e-6)
    }

    @Test
    fun `bad event during sleep only affects time after the event`() {
        // 0~6시간 +6/분, 6시간 시점 악재로 -6/분, 8시간에 수령
        val p = planet().applyEvent(at = 360 * minute, loss = 1.0)
        assertEquals(6.0 * 360 - 6.0 * 120, p.pendingProfitAt(480 * minute), 1e-6)
    }

    @Test
    fun `multiple events each bank their own segment`() {
        val p = planet()
            .applyEvent(at = 60 * minute, newMultiplier = 2.0)   // 0~60분: 6/분
            .applyEvent(at = 120 * minute, newMultiplier = 0.5)  // 60~120분: 12/분
        // 120~180분: 3/분
        assertEquals(6.0 * 60 + 12.0 * 60 + 3.0 * 60, p.pendingProfitAt(180 * minute), 1e-6)
    }

    @Test
    fun `offline cap still counts only the first 24 hours`() {
        val cap = GameConstants.MAX_OFFLINE_MINUTES
        // 상한(24h)을 넘긴 30시간 시점에 악재 — 이미 24시간치가 다 찼으므로 악재 이후 몫은 0
        val late = planet().applyEvent(at = 30 * 60 * minute, loss = 1.0)
        assertEquals(6.0 * cap, late.pendingProfitAt(32 * 60 * minute), 1e-6)

        // 20시간 시점 악재 → 남은 4시간만 악재 생산량으로
        val early = planet().applyEvent(at = 20 * 60 * minute, loss = 1.0)
        assertEquals(6.0 * 1200 - 6.0 * 240, early.pendingProfitAt(32 * 60 * minute), 1e-6)
    }
}
