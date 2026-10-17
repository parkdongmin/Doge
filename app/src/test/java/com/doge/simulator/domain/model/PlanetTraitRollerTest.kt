package com.doge.simulator.domain.model

import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

// 특성 굴리기 규칙과 티어별 결과 분포 — 기대값은 balance.md 3절(확률표)·7절("앱 계산 기준" 표, sim/additive_tiers.js)
class PlanetTraitRollerTest {

    private val samples = 20_000

    @Test
    fun `tiers up to 10 never get traits`() {
        val random = Random(1)
        for (tier in 1..10) repeat(200) {
            assertTrue(PlanetTraitRoller.roll(PlanetType.STAR, tier, random).isEmpty())
        }
    }

    @Test
    fun `traits never repeat or pair with their opposite`() {
        val random = Random(2)
        for (tier in 11..15) repeat(5_000) {
            val traits = PlanetTraitRoller.roll(PlanetType.entries.random(random), tier, random)
            assertTrue("$traits", traits.size in 1..2)
            if (traits.size == 2) assertTrue("$traits", !traits[0].conflictsWith(traits[1].type))
            traits.forEach { assertTrue("$it", if (it.type.isGood) it.intensity in 1..3 else it.intensity == 1) }
        }
    }

    @Test
    fun `slot counts, bad share and grades follow the tier table`() {
        // balance.md 3절: (2개 확률, 나쁜 특성 확률, 좋은 특성 Ⅰ/Ⅱ/Ⅲ)
        val table = mapOf(
            11 to Triple(0.00, 0.25, listOf(0.85, 0.15, 0.00)),
            12 to Triple(0.15, 0.22, listOf(0.70, 0.27, 0.03)),
            13 to Triple(0.30, 0.18, listOf(0.55, 0.37, 0.08)),
            14 to Triple(0.50, 0.14, listOf(0.45, 0.40, 0.15)),
            15 to Triple(0.70, 0.10, listOf(0.35, 0.43, 0.22))
        )
        val random = Random(3)
        for ((tier, expected) in table) {
            var two = 0
            val all = mutableListOf<PlanetTrait>()
            repeat(samples) {
                val traits = PlanetTraitRoller.roll(PlanetType.entries.random(random), tier, random)
                if (traits.size == 2) two++
                all += traits
            }
            assertEquals("T$tier 2개", expected.first, two.toDouble() / samples, 0.02)
            // 충돌로 다시 뽑힌 칸이 섞여 칸 단위 비율은 표보다 약간 흔들린다 — 같은 ±2%p로 본다
            assertEquals("T$tier 나쁜 특성", expected.second, all.count { !it.type.isGood }.toDouble() / all.size, 0.02)
            val good = all.filter { it.type.isGood }
            for (grade in 1..3) {
                assertEquals("T$tier ${grade}등급", expected.third[grade - 1], good.count { it.intensity == grade }.toDouble() / good.size, 0.02)
            }
        }
    }

    // SC-004 — 행성 종류부터 티어 희귀도로 굴리고, 앱과 같은 시세 보정(효과 % 단순 합)으로 판정한다
    @Test
    fun `tier outcomes match the app-calculation table`() {
        // balance.md 7절 앱 계산 기준: 꽝(<0) / 무난(0~10) / 좋음(10~20) / 대박(≥20) %, 평균 %
        val expected = mapOf(
            11 to listOf(25.0, 59.0, 14.0, 1.0, 2.7),
            12 to listOf(23.0, 53.0, 20.0, 4.0, 4.3),
            13 to listOf(19.0, 47.0, 25.0, 9.0, 6.8),
            14 to listOf(14.0, 39.0, 30.0, 16.0, 9.8),
            15 to listOf(10.0, 30.0, 34.0, 26.0, 13.4)
        )
        val random = Random(4)
        val generator = GeneratePlanetsUseCase()
        var minAdj = Double.MAX_VALUE
        var maxAdj = -Double.MAX_VALUE
        for ((tier, e) in expected) {
            val buckets = IntArray(4)
            var sum = 0.0
            repeat(samples) {
                val type = generator.rollPlanetType(tier, random)
                val planet = generator.discoveredPlanet(type, tier, random)
                // 효과 값이 정수 %라 합이 0·10·20에 딱 걸리는 경우가 많다 — 부동소수점 오차로 경계 반대편에 가지 않게 정리
                val adj = Math.round(planet.traitEffects.priceAdjustment * 1e8) / 1e6
                sum += adj
                minAdj = minOf(minAdj, adj)
                maxAdj = maxOf(maxAdj, adj)
                buckets[when { adj < 0 -> 0; adj < 10 -> 1; adj < 20 -> 2; else -> 3 }]++
            }
            for (i in 0..3) assertEquals("T$tier 구간$i", e[i], buckets[i] * 100.0 / samples, 2.0)
            assertEquals("T$tier 평균", e[4], sum / samples, 2.0)
        }
        // FR-010 시세 보정 범위(대략 −25% ~ +75%)
        assertTrue("최소 $minAdj", minAdj >= -30.0)
        assertTrue("최대 $maxAdj", maxAdj <= 75.0)
    }
}
