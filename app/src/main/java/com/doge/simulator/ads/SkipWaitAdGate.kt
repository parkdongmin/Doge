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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 대기시간 단축 광고 하루 횟수 제한 — 탐사·훈련이 같은 횟수를 나눠 쓴다(따로면 하루 10번이 돼 다시
// "광고 보는 게임"에 가까워짐). 기기 로컬 자정 리셋, 클라우드 동기화 안 함 — OrbitDailyRewardGate와 같은 방식
@Singleton
class SkipWaitAdGate @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("skip_wait_ad_prefs", Context.MODE_PRIVATE)

    // 탐사 일지·우주인 센터 두 화면이 같은 남은 횟수를 보도록 공유 상태로 노출
    private val _remaining = MutableStateFlow(computeRemaining())
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    // 자정을 넘겨 화면을 다시 열었을 때 갱신용
    fun refresh() {
        _remaining.value = computeRemaining()
    }

    fun recordUse() {
        resetIfNewDay()
        prefs.edit { putInt(KEY_USED_COUNT, prefs.getInt(KEY_USED_COUNT, 0) + 1) }
        _remaining.value = computeRemaining()
    }

    private fun computeRemaining(): Int {
        resetIfNewDay()
        return OrbitDailyRewardPolicy.remaining(
            viewedCountToday = prefs.getInt(KEY_USED_COUNT, 0),
            maxCount = GameConstants.SKIP_WAIT_AD_DAILY_MAX
        )
    }

    private fun resetIfNewDay() {
        val today = dateFormat.format(Date())
        if (OrbitDailyRewardPolicy.shouldReset(prefs.getString(KEY_LAST_RESET_DATE, null), today)) {
            prefs.edit {
                putString(KEY_LAST_RESET_DATE, today)
                putInt(KEY_USED_COUNT, 0)
            }
        }
    }

    private companion object {
        const val KEY_USED_COUNT = "used_count"
        const val KEY_LAST_RESET_DATE = "last_reset_date"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
