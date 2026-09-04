package com.doge.simulator.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 티어 해금이 "사다리"처럼 순서대로만 열리는지, 그리고 "도감 발견 이력"(매도해도 안 줄어드는
 * 영구 기록) 기준으로 판정되는지 검증한다.
 * (2026-09-04: 상위 희귀도 행성을 하나 일찍 주우면 뒤 티어(T5 등)만 열리고 앞 티어(T4)는
 * 안 열리는 "구멍"이 생기던 걸, 앞 티어까지 전부 열려야 뒤 티어도 열리게 고침.
 * 이어서 판정 기준을 "현재 보유 중"에서 "도감에 발견 기록됨"으로 변경 — 보유 개수 기준이면
 * 행성 하나 파는 순간 사다리 위 티어가 전부 같이 잠기는 문제가 있었음)
 */
class GameConstantsTierLadderTest {

    // COMMON 등급 variantId (T3: 1개+, T4: 3개+ 조건에 쓰임)
    private val common1 = "TERRAN_WET-01"
    private val common2 = "TERRAN_WET-02"
    private val common3 = "TERRAN_WET-03"

    // UNCOMMON 등급 variantId (T5: 1개+, T6: 2개+ 조건에 쓰임)
    private val uncommon1 = "GAS_GIANT_1-01"

    @Test
    fun `도감이 비어있으면 T3에서 막힌다`() {
        assertEquals(3, GameConstants.firstLockedTier(emptySet()))
    }

    @Test
    fun `언커먼 발견 기록 1개만 있어도 T4에서 막힌다 - 커먼 3개 조건은 못 채웠으므로`() {
        // 언커먼도 "커먼 이상" 카운트엔 들어가서 T3(커먼 1개+)는 통과하지만,
        // T4(커먼 이상 3개+)는 1개뿐이라 여전히 막혀야 한다 — T5(언커먼 1개+) 자체 조건은
        // 만족해도 사다리 순서상 T4를 못 넘으므로 T5는 열리면 안 된다
        val locked = GameConstants.firstLockedTier(setOf(uncommon1))
        assertEquals(4, locked)
    }

    @Test
    fun `T4까지 채우면 T5로 넘어가고, 언커먼 발견 기록이 있어야 T5를 통과한다`() {
        val threeCommons = setOf(common1, common2, common3)
        // 커먼만 3개 발견 — T4까지는 통과, T5(언커먼 1개+)에서 막힘
        assertEquals(5, GameConstants.firstLockedTier(threeCommons))

        // 커먼 3개 + 언커먼 1개 발견 — T5까지 통과, T6(언커먼 2개+)에서 막힘
        val withUncommon = threeCommons + uncommon1
        assertEquals(6, GameConstants.firstLockedTier(withUncommon))
    }

    @Test
    fun `maxTier 안에서 전부 열려 있으면 null`() {
        val threeCommons = setOf(common1, common2, common3)
        assertNull(GameConstants.firstLockedTier(threeCommons, maxTier = 4))
    }

    @Test
    fun `이미 발견한 기록은 매도해도 줄지 않는다 - 보유 목록이 아니라 도감이 기준`() {
        // 언커먼을 발견해서 T5까지 열어놨다가 그 행성을 전부 팔아버려도(보유 목록에서 사라져도)
        // discoveredVariantIds(도감)는 영향을 안 받으므로 T5는 계속 열려 있어야 한다
        val threeCommons = setOf(common1, common2, common3)
        val discoveredAfterSellingEverything = threeCommons + uncommon1 // 보유는 0개지만 발견 기록은 남음
        assertEquals(6, GameConstants.firstLockedTier(discoveredAfterSellingEverything))
    }
}
