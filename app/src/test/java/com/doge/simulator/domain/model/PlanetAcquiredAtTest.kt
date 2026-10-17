package com.doge.simulator.domain.model

import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

// 발견 행성은 탐사 완료 시각에 만들어진다 — 몇 시간 뒤에 사도 그 사이 생산·이벤트가 소급되면 안 된다
class PlanetAcquiredAtTest {

    @Test
    fun `planet discovered hours ago starts producing only from purchase`() {
        val hour = 3_600_000L
        val discovered = GeneratePlanetsUseCase().discoveredPlanet(PlanetType.STAR, 12, Random(7))
            .copy(acquireTime = 0L, lastProfitTime = 0L, lastEventTime = 0L)
        val now = 10 * hour

        // 고치기 전: 탐사 완료 시각 그대로라 산 순간 10시간치 수익이 쌓여 있었다
        assertEquals(600L, discovered.elapsedProfitMinutes(now))

        val bought = discovered.acquiredAt(now)
        assertEquals(0L, bought.elapsedProfitMinutes(now))
        assertEquals(0.0, bought.pendingProfitAt(now), 1e-9)
        assertEquals(now, bought.acquireTime)
        assertEquals(now, bought.lastEventTime)
        // 시간 말고는 발견 때 확정한 값이 그대로다
        assertEquals(discovered.copy(acquireTime = now, lastProfitTime = now, lastEventTime = now), bought)
    }
}
