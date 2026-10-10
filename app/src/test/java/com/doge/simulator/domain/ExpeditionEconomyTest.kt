package com.doge.simulator.domain

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.PlanetMetaDataTable
import org.junit.Assert.assertTrue
import org.junit.Test

// 탐사 티어별 분당 코인 기대값(기본 성공 보상 + 슬롯이 꽉 찼을 때의 "코인으로 받기") 검증.
// 발견 확률이 탐사 1회당 고정이라 짧은 티어일수록 시간당 발견이 잦다 — 탐사 시간·발견 확률·
// 행성 가격·코인 전환 비율 중 무엇을 바꾸든 저티어 반복이 최적 농사가 되지 않는지 여기서 걸러낸다.
// 실패하면 표(stdout)를 보고 GameConstants.slotFullCoinTierScale 등을 다시 맞춘다
class ExpeditionEconomyTest {

    private data class Row(val tier: Int, val realMinutes: Double, val basePerMin: Double, val slotPerMin: Double) {
        val totalPerMin get() = basePerMin + slotPerMin
    }

    // 티어별 발견 행성 기대 구매가 — 등급 가중치 × 등급 내 종류 균등(rollPlanetType과 동일),
    // 가격식은 basePrice + production×20 + risk×10(ExploreViewModel.presentExpeditionResult와 동일)
    private fun expectedPrice(tier: Int): Double {
        val byRarity = PlanetMetaDataTable.data.values.groupBy { it.rarity }
        return GameConstants.PLANET_RARITY_WEIGHTS.getValue(tier).entries.sumOf { (rarity, weight) ->
            val avg = byRarity[rarity].orEmpty().map { m ->
                m.basePrice + (m.productionMin + m.productionMax) / 2.0 * 20 + (m.riskMin + m.riskMax) / 2.0 * 10
            }.average()
            weight / 100.0 * avg
        }
    }

    // 우주선 등급별(강화 결과와 같은 증가량), 전문가 보너스 없는 보수적 기준
    private fun table(shipGrade: Int, coinRate: Float): List<Row> {
        val speed = minOf(100, GameConstants.SCOUT_SPEED_BASE + (shipGrade - 1) * GameConstants.UPGRADE_SPEED_PER_GRADE)
        val success = minOf(0.95, GameConstants.SCOUT_SUCCESS_RATE_BASE + (shipGrade - 1) * GameConstants.UPGRADE_SUCCESS_RATE_PER_GRADE.toDouble())
        val discoveryPerRun = success * minOf(0.8f, GameConstants.PLANET_DISCOVERY_BASE_CHANCE + GameConstants.PLANET_DISCOVERY_PLANET_CATEGORY_BONUS)
        return (1..10).map { tier ->
            val minutes = (GameConstants.EXPEDITION_BASE_MINUTES.getValue(tier) * (1 - speed / 200.0))
                .coerceAtLeast(GameConstants.EXPEDITION_MIN_DURATION_MS / 60_000.0)
            val base = success * GameConstants.expeditionSuccessCoinReward(tier)
            val slot = discoveryPerRun * expectedPrice(tier) * coinRate * GameConstants.slotFullCoinTierScale(tier)
            Row(tier, minutes, base / minutes, slot / minutes)
        }
    }

    private fun print(label: String, rows: List<Row>) {
        println("== $label")
        rows.forEach { r ->
            println("T${r.tier} ${"%.1f".format(r.realMinutes)}분 | 기본 ${"%.1f".format(r.basePerMin)} + 코인받기 ${"%.1f".format(r.slotPerMin)} = ${"%.1f".format(r.totalPerMin)}/분")
        }
    }

    @Test
    fun shortTiersAreNotTheBestCoinFarm() {
        for (grade in listOf(1, 3, 6)) {
            for (rate in listOf(GameConstants.DUPLICATE_PLANET_VARIANT_COIN_RATE, GameConstants.SLOT_FULL_DISCOVERY_COIN_RATE)) {
                val rows = table(grade, rate)
                print("우주선 ${grade}등급 · 전환율 $rate", rows)
                val t5 = rows.first { it.tier == 5 }.totalPerMin
                rows.filter { it.tier < 5 }.forEach { r ->
                    assertTrue("T${r.tier}(${r.totalPerMin}/분)이 T5(${t5}/분)보다 효율이 높음 — 우주선 $grade 등급, 전환율 $rate",
                        r.totalPerMin <= t5)
                }
            }
        }
    }

    @Test
    fun baseRewardPerMinuteNeverDropsAtHigherTiers() {
        // 기본 성공 보상(과 같은 배율을 쓰는 자원량)은 티어가 오를수록 분당 효율이 줄면 안 된다
        val rows = table(shipGrade = 1, coinRate = 0f)
        rows.zipWithNext().forEach { (lower, higher) ->
            assertTrue("T${higher.tier} 기본 보상 ${higher.basePerMin}/분 < T${lower.tier} ${lower.basePerMin}/분",
                higher.basePerMin >= lower.basePerMin - 0.05)
        }
    }

    @Test
    fun slotFullCoinsStayABonusOnShortTiers() {
        // 1~2티어에서 "코인으로 받기"가 기본 보상을 압도하면 결과 창의 핵심이 행성이 아니라 환전이 된다
        val rows = table(shipGrade = 1, coinRate = GameConstants.DUPLICATE_PLANET_VARIANT_COIN_RATE)
        rows.filter { it.tier <= 2 }.forEach { r ->
            assertTrue("T${r.tier} 코인받기 ${r.slotPerMin}/분이 기본 ${r.basePerMin}/분의 1.5배 초과",
                r.slotPerMin <= r.basePerMin * 1.5)
        }
    }
}
