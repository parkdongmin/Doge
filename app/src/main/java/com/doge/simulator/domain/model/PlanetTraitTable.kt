package com.doge.simulator.domain.model

import com.doge.simulator.domain.model.PlanetType.ASTEROID
import com.doge.simulator.domain.model.PlanetType.BLACK_HOLE
import com.doge.simulator.domain.model.PlanetType.GALAXY
import com.doge.simulator.domain.model.PlanetType.GAS_GIANT_1
import com.doge.simulator.domain.model.PlanetType.GAS_GIANT_2
import com.doge.simulator.domain.model.PlanetType.ICE_WORLD
import com.doge.simulator.domain.model.PlanetType.ISLANDS
import com.doge.simulator.domain.model.PlanetType.LAVA_WORLD
import com.doge.simulator.domain.model.PlanetType.NO_ATMOSPHERE
import com.doge.simulator.domain.model.PlanetType.STAR
import com.doge.simulator.domain.model.PlanetType.TERRAN_DRY
import com.doge.simulator.domain.model.PlanetType.TERRAN_WET
import com.doge.simulator.domain.model.TraitFit.HIGH
import com.doge.simulator.domain.model.TraitFit.LOW
import com.doge.simulator.domain.model.TraitFit.MEDIUM

// 특성 수치표 — specs/004-part2-planet-traits/balance.md를 그대로 옮긴 것(2026-10-13 시뮬레이션 확정값).
// 숫자를 바꾸려면 먼저 sim/ 스크립트로 다시 검증하고 balance.md와 함께 고친다
object PlanetTraitTable {

    // 좋은 특성 한 칸: 적합도 + 등급 Ⅰ/Ⅱ/Ⅲ 값 + 등급별 하루 순수익 효과(%)
    class Cell(val fit: TraitFit, val values: List<Double>, val dailyProfitPercent: List<Int>)

    private fun cell(fit: TraitFit, v1: Double, v2: Double, v3: Double, p1: Int, p2: Int, p3: Int) =
        Cell(fit, listOf(v1, v2, v3), listOf(p1, p2, p3))

    // 값의 단위(축마다 다름):
    //  - 높은 생산력: 생산량 +비율 (0.14 = +14%)
    //  - 안정된 환경: 악재 확률 −%p (양수로 저장, 적용할 때 뺀다)
    //  - 튼튼한 구조: 고장 나는 악재폭 기준 +값 (0.025 = 15%p → 17.5%p)
    //  - 호황 체질: 호재 상승폭 +비율
    //  - 풍부한 자원: 자원 드롭량 +비율 (1.5 = ×2.5)
    //  - 강화 적성: 강화 성공률 배율 (1.05 = ×1.05)
    // 하루 순수익 효과(balance.md 5절 하단 표): 강화 적성만 "Lv1→20 강화 시도 감소율"(음수)이라 시세에는 40%만 쓴다
    private val GOOD: Map<PlanetType, Map<TraitType, Cell>> = mapOf(
        TERRAN_WET to good(
            prod = cell(MEDIUM, 0.09, 0.16, 0.25, 6, 12, 18),
            stable = cell(MEDIUM, 2.0, 3.5, 5.0, 7, 12, 17),
            sturdy = cell(LOW, 0.005, 0.015, 0.02, 1, 4, 6),
            boom = cell(HIGH, 0.20, 0.40, 0.60, 10, 16, 21),
            rich = cell(HIGH, 0.40, 0.80, 1.20, 12, 23, 35),
            aptitude = cell(LOW, 1.01, 1.02, 1.03, -3, -7, -9)
        ),
        TERRAN_DRY to good(
            prod = cell(HIGH, 0.14, 0.26, 0.40, 12, 23, 35),
            stable = cell(MEDIUM, 1.5, 3.0, 4.5, 6, 12, 19),
            sturdy = cell(HIGH, 0.025, 0.045, 0.07, 9, 16, 25),
            boom = cell(LOW, 0.05, 0.10, 0.15, 3, 6, 9),
            rich = cell(LOW, 0.10, 0.30, 0.40, 1, 4, 5),
            aptitude = cell(MEDIUM, 1.02, 1.04, 1.06, -7, -12, -17)
        ),
        ISLANDS to good(
            prod = cell(MEDIUM, 0.08, 0.14, 0.22, 7, 12, 18),
            stable = cell(HIGH, 3.0, 6.0, 9.0, 12, 24, 35),
            sturdy = cell(LOW, 0.005, 0.015, 0.02, 1, 5, 6),
            boom = cell(HIGH, 0.20, 0.40, 0.60, 11, 19, 24),
            rich = cell(MEDIUM, 0.30, 0.70, 1.00, 5, 13, 18),
            aptitude = cell(LOW, 1.01, 1.02, 1.03, -3, -7, -9)
        ),
        NO_ATMOSPHERE to good(
            prod = cell(LOW, 0.03, 0.05, 0.08, 2, 4, 6),
            stable = cell(MEDIUM, 1.5, 3.0, 4.5, 6, 11, 17),
            sturdy = cell(HIGH, 0.025, 0.045, 0.07, 8, 14, 22),
            boom = cell(LOW, 0.05, 0.10, 0.15, 3, 6, 9),
            rich = cell(MEDIUM, 0.30, 0.50, 0.80, 7, 11, 18),
            aptitude = cell(HIGH, 1.05, 1.08, 1.13, -15, -22, -32)
        ),
        GAS_GIANT_1 to good(
            prod = cell(HIGH, 0.14, 0.26, 0.40, 12, 23, 35),
            stable = cell(HIGH, 3.5, 6.5, 10.0, 11, 19, 28),
            sturdy = cell(LOW, 0.005, 0.015, 0.02, 1, 5, 7),
            boom = cell(MEDIUM, 0.20, 0.40, 0.60, 7, 11, 14),
            rich = cell(LOW, 0.20, 0.30, 0.50, 3, 4, 7),
            aptitude = cell(MEDIUM, 1.02, 1.04, 1.06, -7, -12, -17)
        ),
        GAS_GIANT_2 to good(
            prod = cell(LOW, 0.02, 0.05, 0.07, 2, 4, 6),
            stable = cell(HIGH, 3.5, 6.5, 10.0, 10, 18, 26),
            sturdy = cell(MEDIUM, 0.02, 0.04, 0.06, 6, 12, 18),
            boom = cell(MEDIUM, 0.20, 0.40, 0.60, 7, 11, 13),
            rich = cell(HIGH, 0.50, 1.00, 1.50, 9, 18, 28),
            aptitude = cell(LOW, 1.01, 1.02, 1.03, -3, -7, -9)
        ),
        ASTEROID to good(
            prod = cell(MEDIUM, 0.07, 0.13, 0.20, 6, 11, 18),
            stable = cell(LOW, 0.5, 1.5, 2.0, 1, 4, 6),
            sturdy = cell(MEDIUM, 0.025, 0.04, 0.065, 7, 11, 18),
            boom = cell(LOW, 0.05, 0.10, 0.15, 2, 4, 6),
            rich = cell(HIGH, 0.50, 1.00, 1.50, 7, 14, 21),
            aptitude = cell(HIGH, 1.05, 1.08, 1.13, -15, -22, -32)
        ),
        ICE_WORLD to good(
            prod = cell(LOW, 0.02, 0.05, 0.07, 2, 4, 6),
            stable = cell(HIGH, 3.5, 6.5, 10.0, 7, 13, 20),
            sturdy = cell(HIGH, 0.025, 0.045, 0.07, 7, 12, 19),
            boom = cell(LOW, 0.15, 0.25, 0.40, 3, 4, 6),
            rich = cell(MEDIUM, 0.50, 1.00, 1.50, 6, 12, 18),
            aptitude = cell(MEDIUM, 1.02, 1.04, 1.06, -7, -12, -17)
        ),
        LAVA_WORLD to good(
            prod = cell(HIGH, 0.13, 0.25, 0.38, 12, 23, 35),
            stable = cell(LOW, 1.0, 1.5, 2.5, 2, 4, 6),
            sturdy = cell(MEDIUM, 0.02, 0.035, 0.055, 7, 12, 18),
            boom = cell(LOW, 0.15, 0.25, 0.40, 3, 5, 6),
            rich = cell(MEDIUM, 0.50, 1.00, 1.50, 4, 9, 13),
            aptitude = cell(HIGH, 1.05, 1.08, 1.13, -15, -22, -32)
        ),
        BLACK_HOLE to good(
            prod = cell(MEDIUM, 0.08, 0.14, 0.22, 6, 11, 18),
            stable = cell(HIGH, 2.5, 5.0, 7.5, 11, 24, 36),
            sturdy = cell(HIGH, 0.02, 0.035, 0.055, 13, 23, 36),
            boom = cell(MEDIUM, 0.15, 0.25, 0.40, 9, 13, 18),
            rich = cell(LOW, 0.10, 0.20, 0.30, 2, 5, 7),
            aptitude = cell(LOW, 1.01, 1.02, 1.03, -3, -7, -9)
        ),
        GALAXY to good(
            prod = cell(HIGH, 0.14, 0.26, 0.40, 12, 22, 34),
            stable = cell(MEDIUM, 3.5, 6.5, 10.0, 6, 11, 16),
            sturdy = cell(LOW, 0.01, 0.02, 0.03, 2, 4, 6),
            boom = cell(LOW, 0.20, 0.40, 0.60, 2, 3, 4),
            rich = cell(MEDIUM, 0.40, 0.80, 1.20, 6, 12, 18),
            aptitude = cell(HIGH, 1.05, 1.08, 1.13, -15, -22, -32)
        ),
        STAR to good(
            prod = cell(HIGH, 0.14, 0.26, 0.40, 12, 23, 35),
            stable = cell(LOW, 1.5, 3.0, 4.5, 2, 4, 6),
            sturdy = cell(MEDIUM, 0.025, 0.045, 0.07, 5, 8, 12),
            boom = cell(LOW, 0.20, 0.40, 0.60, 1, 2, 3),
            rich = cell(HIGH, 0.50, 1.00, 1.50, 7, 14, 20),
            aptitude = cell(MEDIUM, 1.02, 1.04, 1.06, -7, -12, -17)
        )
    )

    private fun good(prod: Cell, stable: Cell, sturdy: Cell, boom: Cell, rich: Cell, aptitude: Cell) = mapOf(
        TraitType.HIGH_PRODUCTION to prod,
        TraitType.STABLE_ENVIRONMENT to stable,
        TraitType.STURDY_STRUCTURE to sturdy,
        TraitType.BOOM_PRONE to boom,
        TraitType.RICH_RESOURCES to rich,
        TraitType.UPGRADE_APTITUDE to aptitude
    )

    // 나쁜 특성 고정값(balance.md 4절) — 단위는 좋은 특성과 같고 방향만 반대
    private val BAD_VALUES: Map<TraitType, Double> = mapOf(
        TraitType.LOW_PRODUCTION to -0.10,
        TraitType.UNSTABLE_ENVIRONMENT to 3.0,
        TraitType.WEAK_STRUCTURE to -0.025,
        TraitType.BARREN_RESOURCES to 0.5,
        TraitType.UPGRADE_INAPTITUDE to 0.95
    )

    // 나쁜 특성의 행성별 하루 순수익 효과(%) — balance.md 6절. 순서: 낮은 생산력, 불안정한 환경, 약한 구조, 메마른 자원
    private val BAD_DAILY_PROFIT: Map<PlanetType, List<Int>> = mapOf(
        TERRAN_WET to listOf(-7, -10, -7, -14),
        TERRAN_DRY to listOf(-9, -12, -8, -7),
        ISLANDS to listOf(-8, -11, -8, -9),
        NO_ATMOSPHERE to listOf(-8, -11, -7, -11),
        GAS_GIANT_1 to listOf(-9, -10, -8, -7),
        GAS_GIANT_2 to listOf(-8, -9, -7, -9),
        ASTEROID to listOf(-9, -9, -7, -7),
        ICE_WORLD to listOf(-9, -7, -6, -6),
        LAVA_WORLD to listOf(-9, -8, -8, -4),
        BLACK_HOLE to listOf(-8, -13, -14, -12),
        GALAXY to listOf(-9, -5, -5, -8),
        STAR to listOf(-9, -4, -4, -7)
    )
    private val BAD_DAILY_PROFIT_ORDER = listOf(
        TraitType.LOW_PRODUCTION, TraitType.UNSTABLE_ENVIRONMENT, TraitType.WEAK_STRUCTURE, TraitType.BARREN_RESOURCES
    )

    // 강화 적성·부적성은 수익이 아니라 강화비라 시세에는 "강화 시도 증감율"의 이 비율만 반영(보수적으로)
    const val UPGRADE_PRICE_WEIGHT = 0.4
    // 강화 부적성(×0.95)은 Lv1→20 강화 시도를 약 20% 늘린다 → 시세 −8%
    private const val UPGRADE_INAPTITUDE_ATTEMPT_INCREASE_PERCENT = 20

    fun cell(type: PlanetType, trait: TraitType): Cell? = GOOD[type]?.get(trait)

    fun fit(type: PlanetType, trait: TraitType): TraitFit? = cell(type, trait)?.fit

    // 특성 하나가 그 축에 주는 값(위 단위). 좋은 특성은 등급별, 나쁜 특성은 고정
    fun value(type: PlanetType, trait: PlanetTrait): Double =
        if (trait.type.isGood) cell(type, trait.type)?.values?.getOrNull(trait.intensity - 1) ?: 0.0
        else BAD_VALUES[trait.type] ?: 0.0

    // 특성 하나의 시세 보정(비율, 0.35 = +35%). 하루 순수익 효과 그대로, 강화 적성·부적성만 40% 환산
    fun priceEffect(type: PlanetType, trait: PlanetTrait): Double {
        val percent: Double = when (trait.type) {
            TraitType.UPGRADE_APTITUDE ->
                -(cell(type, trait.type)?.dailyProfitPercent?.getOrNull(trait.intensity - 1) ?: 0) * UPGRADE_PRICE_WEIGHT
            TraitType.UPGRADE_INAPTITUDE -> -UPGRADE_INAPTITUDE_ATTEMPT_INCREASE_PERCENT * UPGRADE_PRICE_WEIGHT
            else -> if (trait.type.isGood) {
                (cell(type, trait.type)?.dailyProfitPercent?.getOrNull(trait.intensity - 1) ?: 0).toDouble()
            } else {
                val index = BAD_DAILY_PROFIT_ORDER.indexOf(trait.type)
                (BAD_DAILY_PROFIT[type]?.getOrNull(index) ?: 0).toDouble()
            }
        }
        return percent / 100.0
    }
}
