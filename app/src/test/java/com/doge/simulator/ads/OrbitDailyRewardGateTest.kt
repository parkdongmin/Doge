package com.doge.simulator.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// OrbitDailyRewardGate 자체는 android.content.Context(SharedPreferences)에 의존해 이 프로젝트의
// 순수 JUnit 유닛 테스트 범위 밖이다(AdFrequencyGate도 동일한 이유로 별도 유닛 테스트가 없다).
// 대신 리셋/잔여횟수 판단의 핵심 로직을 OrbitDailyRewardPolicy로 분리해 여기서 검증하고,
// 실제 SharedPreferences 연동은 quickstart.md의 수동 QA로 확인한다.
class OrbitDailyRewardGateTest {

    @Test
    fun `resets when the stored date differs from today`() {
        assertTrue(OrbitDailyRewardPolicy.shouldReset(lastResetDate = "2026-09-20", today = "2026-09-21"))
        assertTrue(OrbitDailyRewardPolicy.shouldReset(lastResetDate = null, today = "2026-09-21"))
    }

    @Test
    fun `does not reset on the same day`() {
        assertFalse(OrbitDailyRewardPolicy.shouldReset(lastResetDate = "2026-09-21", today = "2026-09-21"))
    }

    @Test
    fun `remaining count never goes below zero`() {
        assertEquals(5, OrbitDailyRewardPolicy.remaining(viewedCountToday = 0, maxCount = 5))
        assertEquals(0, OrbitDailyRewardPolicy.remaining(viewedCountToday = 5, maxCount = 5))
        assertEquals(0, OrbitDailyRewardPolicy.remaining(viewedCountToday = 9, maxCount = 5))
    }
}
