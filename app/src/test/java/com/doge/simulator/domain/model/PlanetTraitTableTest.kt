package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 특성 수치표가 balance.md의 설계 규칙을 지키는지 — 표를 손으로 고치다 생기는 실수를 잡는다
class PlanetTraitTableTest {

    private val goodTraits = TraitType.GOOD

    @Test
    fun `every planet has two high-fit and two low-fit good traits`() {
        for (type in PlanetType.entries) {
            val fits = goodTraits.map { assertNotNull("$type $it", PlanetTraitTable.fit(type, it)); PlanetTraitTable.fit(type, it)!! }
            assertEquals("$type 높음", 2, fits.count { it == TraitFit.HIGH })
            assertEquals("$type 낮음", 2, fits.count { it == TraitFit.LOW })
        }
    }

    @Test
    fun `higher grade is always stronger`() {
        for (type in PlanetType.entries) for (trait in goodTraits) {
            val v = (1..3).map { PlanetTraitTable.value(type, PlanetTrait(trait, it)) }
            assertTrue("$type $trait 값 $v", v[0] < v[1] && v[1] < v[2])
            val p = PlanetTraitTable.cell(type, trait)!!.dailyProfitPercent.map { kotlin.math.abs(it) }
            assertTrue("$type $trait 효과 $p", p[0] < p[1] && p[1] <= p[2])
        }
    }

    @Test
    fun `side-effect caps hold`() {
        for (type in PlanetType.entries) {
            fun top(trait: TraitType) = PlanetTraitTable.value(type, PlanetTrait(trait, 3))
            assertTrue("$type 튼튼한 구조", top(TraitType.STURDY_STRUCTURE) <= 0.07 + 1e-9)
            assertTrue("$type 호황 체질", top(TraitType.BOOM_PRONE) <= 0.60 + 1e-9)
            assertTrue("$type 풍부한 자원", top(TraitType.RICH_RESOURCES) <= 1.50 + 1e-9)
            assertTrue("$type 안정된 환경", top(TraitType.STABLE_ENVIRONMENT) <= 10.0 + 1e-9)
            assertTrue("$type 높은 생산력", top(TraitType.HIGH_PRODUCTION) <= 0.40 + 1e-9)
        }
    }

    // SC-001 — 강화 적성은 수익이 아니라 강화 시도 감소율이라 단위가 달라 빼고 본다(verify.js 1절과 같은 기준)
    @Test
    fun `grade III profit effect follows fit order with the designed averages`() {
        val byFit = TraitFit.entries.associateWith { mutableListOf<Int>() }
        for (type in PlanetType.entries) for (trait in goodTraits - TraitType.UPGRADE_APTITUDE) {
            val cell = PlanetTraitTable.cell(type, trait)!!
            byFit.getValue(cell.fit).add(cell.dailyProfitPercent[2])
        }
        val avg = byFit.mapValues { (_, v) -> v.average() }
        assertTrue("$avg", avg.getValue(TraitFit.HIGH) > avg.getValue(TraitFit.MEDIUM))
        assertTrue("$avg", avg.getValue(TraitFit.MEDIUM) > avg.getValue(TraitFit.LOW))
        assertEquals(29.0, avg.getValue(TraitFit.HIGH), 3.0)
        assertEquals(17.0, avg.getValue(TraitFit.MEDIUM), 3.0)
        assertEquals(6.0, avg.getValue(TraitFit.LOW), 3.0)
    }

    @Test
    fun `bad traits always lower the price and good traits raise it`() {
        for (type in PlanetType.entries) {
            for (trait in TraitType.BAD) assertTrue("$type $trait", PlanetTraitTable.priceEffect(type, PlanetTrait(trait)) < 0)
            for (trait in goodTraits) assertTrue("$type $trait", PlanetTraitTable.priceEffect(type, PlanetTrait(trait, 1)) > 0)
        }
        // 강화 적성·부적성은 시도 증감율의 40%
        assertEquals(-0.08, PlanetTraitTable.priceEffect(PlanetType.STAR, PlanetTrait(TraitType.UPGRADE_INAPTITUDE)), 1e-9)
        assertEquals(0.128, PlanetTraitTable.priceEffect(PlanetType.GALAXY, PlanetTrait(TraitType.UPGRADE_APTITUDE, 3)), 1e-9)
    }

    @Test
    fun `opposite pairs and encoding round-trip`() {
        assertEquals(TraitType.LOW_PRODUCTION, TraitType.HIGH_PRODUCTION.opposite)
        assertEquals(TraitType.UPGRADE_APTITUDE, TraitType.UPGRADE_INAPTITUDE.opposite)
        assertEquals(null, TraitType.BOOM_PRONE.opposite)

        val traits = listOf(PlanetTrait(TraitType.HIGH_PRODUCTION, 3), PlanetTrait(TraitType.UNSTABLE_ENVIRONMENT))
        assertEquals(traits, PlanetTrait.decode(PlanetTrait.encode(traits)))
        // 모르는 이름·깨진 조각은 건너뛴다
        assertEquals(listOf(PlanetTrait(TraitType.BOOM_PRONE, 2)), PlanetTrait.decode("FUTURE_TRAIT:2,BOOM_PRONE:2,:,"))
        assertEquals(emptyList<PlanetTrait>(), PlanetTrait.decode(""))
    }
}
