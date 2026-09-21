package com.doge.simulator.domain.model.orbit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitRiskTierTest {

    @Test
    fun `lowest tier is always the safe tier`() {
        assertEquals(OrbitRiskTier.SAFE, OrbitRiskTier.LOWEST)
    }

    @Test
    fun `higher tiers have a lower ai mistake rate and higher reward multiplier`() {
        val tiers = listOf(OrbitRiskTier.SAFE, OrbitRiskTier.CHALLENGE, OrbitRiskTier.HIGH_RISK)
        for (i in 0 until tiers.size - 1) {
            val lower = tiers[i]
            val higher = tiers[i + 1]
            assertTrue(
                "${higher.name}의 실수 빈도가 ${lower.name}보다 낮아야 한다",
                higher.aiMistakeRate < lower.aiMistakeRate
            )
            assertTrue(
                "${higher.name}의 보상 배율이 ${lower.name}보다 커야 한다",
                higher.winRewardMultiplier > lower.winRewardMultiplier
            )
        }
    }

    @Test
    fun `all three tiers are always available regardless of wealth`() {
        // 위험도 티어는 재화 규모와 무관한 독립 축이라, enum 자체에 잔고 조건이 없어야 한다(FR-012a).
        assertEquals(3, OrbitRiskTier.entries.size)
    }
}
