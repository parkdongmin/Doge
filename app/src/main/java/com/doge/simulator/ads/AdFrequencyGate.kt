package com.doge.simulator.ads

import android.content.Context
import androidx.core.content.edit
import com.doge.simulator.domain.model.GameConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 탐사 결과 전면광고 빈도 제한: 첫 N회 dismiss는 무조건 건너뛰고, 이후엔 쿨다운이 지나야 노출
@Singleton
class AdFrequencyGate @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("ad_frequency_prefs", Context.MODE_PRIVATE)

    fun shouldShowInterstitial(): Boolean {
        val completedCount = prefs.getInt(KEY_COMPLETED_COUNT, 0)
        if (completedCount < GameConstants.INTERSTITIAL_GRACE_COMPLETIONS) return false
        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
        return System.currentTimeMillis() - lastShownAt >= GameConstants.INTERSTITIAL_COOLDOWN_MS
    }

    fun recordExpeditionCompleted() {
        prefs.edit { putInt(KEY_COMPLETED_COUNT, prefs.getInt(KEY_COMPLETED_COUNT, 0) + 1) }
    }

    fun recordInterstitialShown() {
        prefs.edit { putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis()) }
    }

    private companion object {
        const val KEY_COMPLETED_COUNT = "completed_count"
        const val KEY_LAST_SHOWN_AT = "last_shown_at"
    }
}
