package com.doge.simulator.domain.model

import com.doge.simulator.domain.usecase.GeneratePlanetsUseCase
import com.doge.simulator.domain.usecase.RollPlanetEventUseCase.Companion.applyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 특성 효과가 행성 확장 프로퍼티 한 곳에서 계산되고, 이벤트·정비·가격에 그대로 이어지는지
class PlanetTraitEffectsTest {

    // 항성 중간 스탯: 매입가 200,000 + 550×20 + 14×10 = 211,140 (balance.md 6절 예시와 같은 숫자)
    private fun star(vararg traits: PlanetTrait) = Planet(
        type = PlanetType.STAR,
        production = 550, risk = 14, investment = 0, eventRate = 28, buyPrice = 211_140,
        acquireTime = 0L, lastProfitTime = 0L, lastEventTime = 0L,
        level = 10, traits = traits.toList()
    )

    @Test
    fun `planet without traits keeps every original value`() {
        val p = star()
        assertEquals(550 * 0.15 * GameConstants.planetLevelMultiplier(10), p.normalProduction, 1e-9)
        assertEquals(28, p.effectiveBadChance)
        assertEquals(GameConstants.PLANET_BREAKDOWN_DELTA_THRESHOLD, p.breakdownThreshold, 1e-9)
        assertEquals(1.0, p.boomMultiplier, 1e-9)
        assertEquals(1.0, p.resourceDropMultiplier, 1e-9)
        assertEquals(GameConstants.UPGRADE_SUCCESS_RATES.getValue(10), p.upgradeSuccessRate(), 1e-6f)
        assertEquals(0.0, p.traitEffects.priceAdjustment, 1e-9)
    }

    @Test
    fun `each trait changes only its own axis`() {
        val base = star().normalProduction
        assertEquals(base * 1.40, star(PlanetTrait(TraitType.HIGH_PRODUCTION, 3)).normalProduction, 1e-9)
        assertEquals(base * 0.90, star(PlanetTrait(TraitType.LOW_PRODUCTION)).normalProduction, 1e-9)

        // 항성 안정된 환경은 적합도 낮음: −1.5 / −3 / −4.5 %p
        assertEquals(24, star(PlanetTrait(TraitType.STABLE_ENVIRONMENT, 3)).effectiveBadChance)
        assertEquals(31, star(PlanetTrait(TraitType.UNSTABLE_ENVIRONMENT)).effectiveBadChance)

        assertEquals(0.22, star(PlanetTrait(TraitType.STURDY_STRUCTURE, 3)).breakdownThreshold, 1e-9)
        assertEquals(0.125, star(PlanetTrait(TraitType.WEAK_STRUCTURE)).breakdownThreshold, 1e-9)

        assertEquals(1.6, star(PlanetTrait(TraitType.BOOM_PRONE, 3)).boomMultiplier, 1e-9)
        assertEquals(2.5, star(PlanetTrait(TraitType.RICH_RESOURCES, 3)).resourceDropMultiplier, 1e-9)
        assertEquals(0.5, star(PlanetTrait(TraitType.BARREN_RESOURCES)).resourceDropMultiplier, 1e-9)

        assertEquals(0.50f * 1.06f, star(PlanetTrait(TraitType.UPGRADE_APTITUDE, 3)).upgradeSuccessRate(), 1e-6f)
        assertEquals(0.50f * 0.95f, star(PlanetTrait(TraitType.UPGRADE_INAPTITUDE)).upgradeSuccessRate(), 1e-6f)
    }

    @Test
    fun `bad chance and upgrade success stay in range`() {
        val calm = Planet(
            type = PlanetType.GAS_GIANT_1, production = 200, risk = 30, investment = 0, eventRate = 5, buyPrice = 1,
            traits = listOf(PlanetTrait(TraitType.STABLE_ENVIRONMENT, 3))
        )
        assertEquals(0, calm.effectiveBadChance)
        // 레벨 1 성공률 95% × 1.13 — 100%를 넘지 않게 99%에서 멈춘다
        val apt = Planet(
            type = PlanetType.GALAXY, production = 300, risk = 20, investment = 0, eventRate = 30, buyPrice = 1, level = 1,
            traits = listOf(PlanetTrait(TraitType.UPGRADE_APTITUDE, 3))
        )
        assertEquals(0.99f, apt.upgradeSuccessRate(), 1e-6f)
    }

    @Test
    fun `sturdy and weak structure move the breakdown line`() {
        val sturdy = applyEvent(star(PlanetTrait(TraitType.STURDY_STRUCTURE, 3)), isBad = true, delta = 0.20)
        assertFalse("기준 22%p라 20%p 악재엔 안 고장", sturdy.isBroken)
        val weak = applyEvent(star(PlanetTrait(TraitType.WEAK_STRUCTURE)), isBad = true, delta = 0.13)
        assertTrue("기준 12.5%p라 13%p 악재에 고장", weak.isBroken)
        val plain = applyEvent(star(), isBad = true, delta = 0.13)
        assertFalse(plain.isBroken)
    }

    @Test
    fun `boom prone raises good events and leaves broken planets alone`() {
        val boomed = applyEvent(star(PlanetTrait(TraitType.BOOM_PRONE, 3)), isBad = false, delta = 0.10)
        assertEquals(1.16, boomed.productionMultiplier, 1e-9)
        val plain = applyEvent(star(), isBad = false, delta = 0.10)
        assertEquals(1.10, plain.productionMultiplier, 1e-9)

        // 고장 중 호재는 손해 배율만 줄인다 — 호황 체질과 무관
        val broken = star(PlanetTrait(TraitType.BOOM_PRONE, 3)).copy(lossMultiplier = 1.0)
        assertEquals(0.9, applyEvent(broken, isBad = false, delta = 0.10).lossMultiplier, 1e-9)
    }

    @Test
    fun `maintenance and breakdown loss follow trait production`() {
        val plain = star()
        val strong = star(PlanetTrait(TraitType.HIGH_PRODUCTION, 3))
        assertEquals(plain.maintenanceCost * 1.4, strong.maintenanceCost.toDouble(), 2.0)
        val brokenStrong = strong.copy(lossMultiplier = 1.0)
        assertEquals(-strong.normalProduction, brokenStrong.preciseProduction, 1e-9)
    }

    @Test
    fun `trait price is baked into the discovery price`() {
        val traits = listOf(PlanetTrait(TraitType.HIGH_PRODUCTION, 3))
        // 항성 + 높은 생산력 Ⅲ(적합도 높음, +35%) → 약 285,000
        val price = GeneratePlanetsUseCase.discoveryPrice(PlanetType.STAR, production = 550, risk = 14, traits = traits)
        assertEquals(285_039, price)
        assertEquals(0.35, star(*traits.toTypedArray()).traitEffects.priceAdjustment, 1e-9)
        assertEquals(211_140, GeneratePlanetsUseCase.discoveryPrice(PlanetType.STAR, 550, 14, emptyList()))
    }
}
