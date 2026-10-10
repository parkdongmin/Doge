package com.doge.simulator.ads

import android.content.Context
import androidx.core.content.edit
import com.doge.simulator.domain.model.GameConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 탐사 결과 전면광고 빈도 제한: 첫 N회 dismiss는 무조건 건너뛰고, 이후엔 마지막 광고 이후
// 탐사 결과를 INTERVAL번 처리할 때마다 노출
@Singleton
class AdFrequencyGate @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("ad_frequency_prefs", Context.MODE_PRIVATE)

    fun shouldShowInterstitial(): Boolean {
        val completedCount = prefs.getInt(KEY_COMPLETED_COUNT, 0)
        if (completedCount <= GameConstants.INTERSTITIAL_GRACE_COMPLETIONS) return false
        return prefs.getInt(KEY_SINCE_LAST_SHOWN, 0) >= GameConstants.INTERSTITIAL_INTERVAL_COMPLETIONS
    }

    fun recordExpeditionCompleted() {
        prefs.edit {
            putInt(KEY_COMPLETED_COUNT, prefs.getInt(KEY_COMPLETED_COUNT, 0) + 1)
            putInt(KEY_SINCE_LAST_SHOWN, prefs.getInt(KEY_SINCE_LAST_SHOWN, 0) + 1)
        }
    }

    fun recordInterstitialShown() {
        prefs.edit { putInt(KEY_SINCE_LAST_SHOWN, 0) }
    }

    private companion object {
        const val KEY_COMPLETED_COUNT = "completed_count"
        const val KEY_SINCE_LAST_SHOWN = "since_last_shown"
    }
}
