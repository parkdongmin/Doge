package com.doge.simulator.domain.usecase

import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.preciseProduction
import javax.inject.Inject

data class PendingProfit(val coins: Long, val maxElapsedMinutes: Long)

// "복귀 시 확인 다이얼로그"에 보여줄 미수령 수익을 리포지토리에 쓰지 않고 미리 계산만 한다.
// 실제 지급은 CollectProfitUseCase(배율 포함)가 담당한다
class PeekPendingProfitUseCase @Inject constructor() {
    operator fun invoke(planets: List<Planet>): PendingProfit {
        val now = System.currentTimeMillis()
        var totalEarned = 0L
        var maxElapsedMinutes = 0L

        planets.forEach { planet ->
            val rawElapsed = (now - planet.lastProfitTime) / 60_000L
            val elapsedMinutes = minOf(rawElapsed, GameConstants.MAX_OFFLINE_MINUTES)
            if (elapsedMinutes <= 0) return@forEach
            totalEarned += (planet.preciseProduction * elapsedMinutes).toLong()
            maxElapsedMinutes = maxOf(maxElapsedMinutes, elapsedMinutes)
        }

        return PendingProfit(coins = totalEarned, maxElapsedMinutes = maxElapsedMinutes)
    }
}
