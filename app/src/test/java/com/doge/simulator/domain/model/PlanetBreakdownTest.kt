package com.doge.simulator.domain.model

import com.doge.simulator.domain.usecase.RollPlanetEventUseCase.Companion.applyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 행성 고장/정비 규칙. production 60 × 0.15 = 레벨 1·배율 1.0 기준 분당 9코인,
// 매입가 2,260(무대기 행성 중간 스탯) — 설계 표의 예시와 같은 숫자
class PlanetBreakdownTest {

    private fun planet() = Planet(
        type = PlanetType.NO_ATMOSPHERE,
        production = 60, risk = 6, investment = 0, eventRate = 50, buyPrice = 2260,
        acquireTime = 0L, lastProfitTime = 0L, lastEventTime = 0L
    )

    @Test
    fun `good event on a healthy planet raises the multiplier`() {
        val p = applyEvent(planet(), isBad = false, delta = 0.2)
        assertFalse(p.isBroken)
        assertEquals(10.8, p.preciseProduction, 1e-9)
        assertEquals(2712L, p.marketValue)
    }

    @Test
    fun `bad event on a healthy planet cuts the multiplier then breaks it`() {
        val boosted = applyEvent(planet(), isBad = false, delta = 0.2)
        val broken = applyEvent(boosted, isBad = true, delta = 0.2)
        assertTrue(broken.isBroken)
        assertEquals(1.0, broken.productionMultiplier, 1e-9)
        assertEquals(-9.0, broken.preciseProduction, 1e-9)
        assertEquals(1582L, broken.marketValue) // −30%
    }

    @Test
    fun `small bad event on a healthy planet only cuts the multiplier`() {
        val p = applyEvent(planet(), isBad = true, delta = 0.1)
        assertFalse(p.isBroken)
        assertEquals(0.9, p.productionMultiplier, 1e-9)
        assertEquals(8.1, p.preciseProduction, 1e-9)
    }

    @Test
    fun `events on a broken planet only move the loss`() {
        val broken = applyEvent(planet(), isBad = true, delta = 0.15).copy(productionMultiplier = 1.0, marketAdjustment = 0L)
        val worse = applyEvent(broken, isBad = true, delta = 0.2)
        assertEquals(-10.8, worse.preciseProduction, 1e-9)
        assertEquals(1446L, worse.marketValue) // −36%
        assertEquals(1.0, worse.productionMultiplier, 1e-9)

        // 호재는 손해만 줄이고 플러스로 돌려놓지 않는다
        val better = applyEvent(worse, isBad = false, delta = 0.1)
        assertTrue(better.isBroken)
        assertEquals(-9.9, better.preciseProduction, 1e-9)
    }

    @Test
    fun `multipliers stay within their ranges`() {
        var p = planet()
        repeat(20) { p = applyEvent(p, isBad = false, delta = 0.3) }
        assertEquals(GameConstants.PLANET_EVENT_MULTIPLIER_CEILING, p.productionMultiplier, 1e-9)

        p = applyEvent(p, isBad = true, delta = 0.3)
        repeat(20) { p = applyEvent(p, isBad = true, delta = 0.3) }
        assertEquals(GameConstants.PLANET_LOSS_MULTIPLIER_MAX, p.lossMultiplier, 1e-9)
        repeat(20) { p = applyEvent(p, isBad = false, delta = 0.3) }
        assertEquals(GameConstants.PLANET_LOSS_MULTIPLIER_MIN, p.lossMultiplier, 1e-9)
        assertTrue(p.isBroken)
    }

    @Test
    fun `maintenance cost is two hours of normal production and ignores the loss`() {
        val broken = planet().copy(lossMultiplier = 1.7)
        assertEquals(1080L, broken.maintenanceCost)
        assertEquals(9.0, broken.copy(lossMultiplier = 0.0).preciseProduction, 1e-9)
    }
}
