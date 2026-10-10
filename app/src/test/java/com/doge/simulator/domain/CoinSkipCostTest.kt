package com.doge.simulator.domain

import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionCategory
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.RarityTier
import com.doge.simulator.domain.model.ResearchLab
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.domain.model.coinSkipCost
import com.doge.simulator.domain.model.coinSkipUnitPerMinute
import com.doge.simulator.domain.model.expectedExpeditionValue
import com.doge.simulator.domain.model.expeditionDurationMs
import org.junit.Assert.assertTrue
import org.junit.Test

// 코인 단축 비용 검증 — 성장 단계(초·중·후반)별로 실제 비용 함수(coinSkipCost)를 돌려
// ① 탐사 1회를 통째로 단축해도 그 탐사의 기대 가치보다 항상 비싼지(단축으로 돈 버는 구멍 없음)
// ② 같은 시간의 행성 방치 수입보다 항상 비싼지(광고 5회가 여전히 매력적인지)
// 를 본다. 실패하면 stdout 표를 보고 GameConstants.COIN_SKIP_PREMIUM 등을 다시 맞춘다
class CoinSkipCostTest {

    private data class Stage(
        val name: String,
        val planets: List<Triple<RarityTier, Int, Int>>, // 등급, 개수, 강화 레벨
        val shipGrade: Int,
        val crew: Int,
        val matching: Int,
        val matchingProficiency: Int,
        val tiers: List<Int>
    )

    private val stages = listOf(
        Stage("초반", listOf(Triple(RarityTier.COMMON, 3, 3)), 1, 2, 1, 20, listOf(1, 2, 3)),
        Stage("중반", listOf(Triple(RarityTier.COMMON, 3, 8), Triple(RarityTier.UNCOMMON, 3, 6)), 3, 4, 2, 45, listOf(4, 5, 6)),
        Stage("후반", listOf(Triple(RarityTier.COMMON, 2, 12), Triple(RarityTier.UNCOMMON, 3, 12),
            Triple(RarityTier.RARE, 3, 10), Triple(RarityTier.EPIC, 2, 8)), 6, 8, 4, 80, listOf(7, 8, 10))
    )

    private fun ship(grade: Int) = Spaceship(
        name = "test",
        grade = grade,
        crewCapacity = GameConstants.SCOUT_CREW_BASE + (grade - 1) * GameConstants.UPGRADE_CREW_PER_GRADE,
        speed = minOf(100, GameConstants.SCOUT_SPEED_BASE + (grade - 1) * GameConstants.UPGRADE_SPEED_PER_GRADE),
        cargo = GameConstants.SCOUT_CARGO_BASE + (grade - 1) * GameConstants.UPGRADE_CARGO_PER_GRADE,
        successRate = GameConstants.SCOUT_SUCCESS_RATE_BASE + (grade - 1) * GameConstants.UPGRADE_SUCCESS_RATE_PER_GRADE
    )

    private fun crew(s: Stage): List<Astronaut> = List(s.crew) { i ->
        Astronaut(
            name = "a$i",
            specialty = if (i < s.matching) AstronautSpecialty.PLANET else AstronautSpecialty.MINERAL,
            grade = AstronautGrade.REGULAR,
            proficiency = if (i < s.matching) s.matchingProficiency else 30
        )
    }

    private fun passivePerMin(s: Stage): Double {
        val byRarity = PlanetMetaDataTable.data.values.groupBy { it.rarity }
        return s.planets.sumOf { (rarity, count, level) ->
            val avgProduction = byRarity.getValue(rarity).map { (it.productionMin + it.productionMax) / 2.0 }.average()
            count * avgProduction * GameConstants.PLANET_PRODUCTION_SCALE * GameConstants.planetLevelMultiplier(level)
        }
    }

    @Test
    fun skippingNeverBeatsWaiting() {
        val lab = ResearchLab()
        for (s in stages) {
            val ship = ship(s.shipGrade)
            val team = crew(s)
            val passive = passivePerMin(s)
            println("== ${s.name}: 행성 방치 ${"%.1f".format(passive)}코인/분")
            for (tier in s.tiers) {
                val durationMs = expeditionDurationMs(tier, ship)
                val expedition = Expedition(
                    category = ExpeditionCategory.PLANET, tier = tier, astronautIds = team.map { it.id },
                    spaceshipId = ship.id, startTime = 0L, endTime = durationMs
                )
                val unit = coinSkipUnitPerMinute(expedition, ship, team, lab, passive)
                val fullCost = coinSkipCost(durationMs, unit)
                val value = expectedExpeditionValue(tier, ship, team, ExpeditionCategory.PLANET, lab)
                val passiveDuring = passive * durationMs / 60_000.0
                println("T$tier ${durationMs / 60_000}분 | 전체 단축 ${"%,d".format(fullCost)} | 탐사 기대가치 ${"%,.0f".format(value)} | 같은 시간 방치 ${"%,.0f".format(passiveDuring)}")

                assertTrue("${s.name} T$tier: 단축 비용 $fullCost < 탐사 기대가치의 1.5배(${value * 1.5})",
                    fullCost >= value * 1.5)
                assertTrue("${s.name} T$tier: 단축 비용 $fullCost < 같은 시간 방치 수입의 1.5배(${passiveDuring * 1.5})",
                    fullCost >= passiveDuring * 1.5)
            }
        }
    }

    @Test
    fun costShrinksAsTimePasses() {
        // 남은 시간이 줄수록 비용도 줄고, 최소 1코인
        val unit = 100.0
        assertTrue(coinSkipCost(60 * 60_000L, unit) > coinSkipCost(30 * 60_000L, unit))
        assertTrue(coinSkipCost(1L, unit) >= 1L)
    }
}
