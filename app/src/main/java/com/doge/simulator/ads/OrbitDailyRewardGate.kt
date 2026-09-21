package com.doge.simulator.ads

import android.content.Context
import androidx.core.content.edit
import com.doge.simulator.domain.model.GameConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// 휴게실 일일 리워드 광고 횟수 제한: 기기 로컬 자정 기준으로 하루 최대
// GameConstants.ORBIT_DAILY_AD_MAX_COUNT회. 클라우드 세이브와 동기화하지 않는다(설계 확정 사항 —
// 어뷰징 방지 비용 대비 실익이 낮다고 판단). AdFrequencyGate와 동일한 SharedPreferences 패턴.
@Singleton
class OrbitDailyRewardGate @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("orbit_daily_reward_prefs", Context.MODE_PRIVATE)

    fun remainingToday(): Int {
        resetIfNewDay()
        return OrbitDailyRewardPolicy.remaining(
            viewedCountToday = prefs.getInt(KEY_VIEWED_COUNT, 0),
            maxCount = GameConstants.ORBIT_DAILY_AD_MAX_COUNT
        )
    }

    fun canClaimToday(): Boolean = remainingToday() > 0

    fun recordClaim() {
        resetIfNewDay()
        prefs.edit { putInt(KEY_VIEWED_COUNT, prefs.getInt(KEY_VIEWED_COUNT, 0) + 1) }
    }

    private fun resetIfNewDay() {
        val today = todayKey()
        if (OrbitDailyRewardPolicy.shouldReset(prefs.getString(KEY_LAST_RESET_DATE, null), today)) {
            prefs.edit {
                putString(KEY_LAST_RESET_DATE, today)
                putInt(KEY_VIEWED_COUNT, 0)
            }
        }
    }

    private fun todayKey(): String = dateFormat.format(Date())

    private companion object {
        const val KEY_VIEWED_COUNT = "viewed_count"
        const val KEY_LAST_RESET_DATE = "last_reset_date"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
