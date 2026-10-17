package com.doge.simulator.domain.model

import kotlin.random.Random

// 발견한 행성에 붙일 특성을 굴린다 — balance.md 3절 확률표. T10 이하는 항상 특성 없음.
// 절차는 수치를 만든 시뮬레이션(sim/verify.js 4절)과 같다: 개수 → 칸마다 좋음/나쁨 → 종류(균등) → 등급,
// 이미 뽑힌 특성과 같거나 반대 짝이면 그 칸을 처음부터 다시 뽑는다
object PlanetTraitRoller {

    const val FIRST_TRAIT_TIER = 11

    private class TierOdds(val twoTraitChance: Double, val badChance: Double, val gradeWeights: List<Int>)

    private val ODDS: Map<Int, TierOdds> = mapOf(
        11 to TierOdds(0.00, 0.25, listOf(85, 15, 0)),
        12 to TierOdds(0.15, 0.22, listOf(70, 27, 3)),
        13 to TierOdds(0.30, 0.18, listOf(55, 37, 8)),
        14 to TierOdds(0.50, 0.14, listOf(45, 40, 15)),
        15 to TierOdds(0.70, 0.10, listOf(35, 43, 22))
    )

    // 반대·중복 다시 뽑기의 안전장치 — 후보가 넉넉해 실제로는 몇 번 안에 끝난다
    private const val MAX_REROLLS = 20

    fun roll(type: PlanetType, tier: Int, random: Random = Random): List<PlanetTrait> {
        if (tier < FIRST_TRAIT_TIER) return emptyList()
        val odds = ODDS[tier] ?: ODDS.getValue(ODDS.keys.max())
        val count = if (random.nextDouble() < odds.twoTraitChance) 2 else 1

        val picked = mutableListOf<PlanetTrait>()
        var attempts = 0
        while (picked.size < count && attempts < MAX_REROLLS) {
            attempts++
            val isBad = random.nextDouble() < odds.badChance
            val trait = if (isBad) {
                PlanetTrait(TraitType.BAD[random.nextInt(TraitType.BAD.size)])
            } else {
                PlanetTrait(TraitType.GOOD[random.nextInt(TraitType.GOOD.size)], pickGrade(odds.gradeWeights, random))
            }
            if (picked.any { it.conflictsWith(trait.type) }) continue
            picked += trait
        }
        return picked
    }

    private fun pickGrade(weights: List<Int>, random: Random): Int {
        var roll = random.nextInt(weights.sum())
        weights.forEachIndexed { index, weight ->
            roll -= weight
            if (roll < 0) return index + 1
        }
        return weights.size
    }
}
