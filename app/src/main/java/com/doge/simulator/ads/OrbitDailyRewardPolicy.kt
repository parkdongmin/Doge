package com.doge.simulator.ads

// OrbitDailyRewardGate의 순수 로직만 분리한 것 — Android Context/SharedPreferences 없이도
// "언제 리셋되는지"와 "몇 회 남았는지"를 유닛 테스트로 검증할 수 있게 하기 위함.
internal object OrbitDailyRewardPolicy {
    fun shouldReset(lastResetDate: String?, today: String): Boolean = lastResetDate != today

    fun remaining(viewedCountToday: Int, maxCount: Int): Int =
        (maxCount - viewedCountToday).coerceAtLeast(0)
}
