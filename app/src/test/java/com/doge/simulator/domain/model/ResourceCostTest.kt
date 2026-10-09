package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 자원 소비처(강화·정비) 비용 규칙. 숫자는 2026-10-09 수급 시뮬레이션으로 확정한 표와 같다
class ResourceCostTest {

    private fun planet(type: PlanetType = PlanetType.NO_ATMOSPHERE, level: Int = 1) = Planet(
        type = type,
        production = 60, risk = 6, investment = 0, eventRate = 50, buyPrice = 2260,
        level = level, acquireTime = 0L, lastProfitTime = 0L, lastEventTime = 0L
    )

    @Test
    fun `upgrade resources up to level 10 come only from the starter planet`() {
        val starterDrops = PlanetMetaDataTable.data.getValue(PlanetType.NO_ATMOSPHERE).resourceDrops.keys
        for (level in 1..10) {
            val (_, resources) = GameConstants.planetUpgradeCost(level, RarityTier.COMMON)
            assertTrue("Lv.$level uses ${resources.keys}", starterDrops.containsAll(resources.keys))
        }
    }

    @Test
    fun `upgrade resources scale with planet rarity but coins do not`() {
        val (commonCoins, common) = GameConstants.planetUpgradeCost(9, RarityTier.COMMON)
        val (uncommonCoins, uncommon) = GameConstants.planetUpgradeCost(9, RarityTier.UNCOMMON)
        assertEquals(commonCoins, uncommonCoins)
        assertEquals(mapOf(ResourceType.IRON_ORE to 100, ResourceType.CRYSTAL to 60, ResourceType.NANOBOT to 5), common)
        assertEquals(mapOf(ResourceType.IRON_ORE to 150, ResourceType.CRYSTAL to 90, ResourceType.NANOBOT to 8), uncommon)

        val star = planet(PlanetType.STAR, level = 16)
        assertEquals(280, star.upgradeCost.second[ResourceType.ENERGY_CORE])
    }

    @Test
    fun `drop chance exists for every danger level and falls as success rate falls`() {
        val levels = GameConstants.DANGER_ZONE_START until GameConstants.PLANET_MAX_LEVEL
        val chances = levels.map { GameConstants.UPGRADE_DROP_CHANCES.getValue(it) }
        assertTrue(chances.zipWithNext().all { (a, b) -> a > b })
    }

    @Test
    fun `maintenance resources grow with the square of the level multiplier`() {
        assertEquals(mapOf(ResourceType.IRON_ORE to 15L), planet(level = 1).maintenanceResourceCost)
        assertEquals(mapOf(ResourceType.IRON_ORE to 60L), planet(level = 11).maintenanceResourceCost)
        assertEquals(
            mapOf(ResourceType.IRON_ORE to 154L, ResourceType.ENERGY_CORE to 110L, ResourceType.NANOBOT to 44L),
            planet(PlanetType.STAR, level = 12).maintenanceResourceCost
        )
    }

    @Test
    fun `missing maintenance resources are paid in coins at twice the sell price`() {
        val broken = planet().copy(lossMultiplier = 1.0)
        val plan = broken.maintenancePlan { type -> if (type == ResourceType.IRON_ORE) 10L else 0L }
        assertEquals(mapOf(ResourceType.IRON_ORE to 10L), plan.resourcesUsed)
        assertEquals(mapOf(ResourceType.IRON_ORE to 5L), plan.shortfall)
        assertEquals(90L, plan.substituteCoins) // 5개 × 판매가 9 × 2
        assertEquals(810L + 90L, plan.totalCoins)

        val stocked = broken.maintenancePlan { 1_000L }
        assertEquals(0L, stocked.substituteCoins)
        assertEquals(810L, stocked.totalCoins)
    }
}
