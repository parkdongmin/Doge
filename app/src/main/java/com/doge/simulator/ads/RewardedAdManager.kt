package com.doge.simulator.ads

import android.app.Activity
import android.content.Context
import com.doge.simulator.BuildConfig
import com.doge.simulator.audio.BgmPlayer
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class RewardPlacement {
    POOL_REFRESH, OFFLINE_PROFIT_X2, SKIP_WAIT, UPGRADE_REVERT, ORBIT_DAILY
}

sealed class RewardedAdResult {
    object Earned : RewardedAdResult()
    object NotReady : RewardedAdResult()
    object Dismissed : RewardedAdResult()
    data class Failed(val message: String) : RewardedAdResult()
}

@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bgmPlayer: BgmPlayer
) {
    private val loadedAds = mutableMapOf<RewardPlacement, RewardedAd>()
    private val loadingPlacements = mutableSetOf<RewardPlacement>()

    private fun adUnitIdFor(placement: RewardPlacement): String = when (placement) {
        RewardPlacement.POOL_REFRESH -> BuildConfig.AD_UNIT_REWARD_REFRESH
        RewardPlacement.OFFLINE_PROFIT_X2 -> BuildConfig.AD_UNIT_REWARD_OFFLINE_X2
        RewardPlacement.SKIP_WAIT -> BuildConfig.AD_UNIT_REWARD_SKIP_WAIT
        RewardPlacement.UPGRADE_REVERT -> BuildConfig.AD_UNIT_REWARD_UPGRADE_REVERT
        RewardPlacement.ORBIT_DAILY -> BuildConfig.AD_UNIT_REWARD_ORBIT_DAILY
    }

    fun preload(placement: RewardPlacement) {
        if (loadedAds.containsKey(placement) || loadingPlacements.contains(placement)) return
        loadingPlacements.add(placement)
        RewardedAd.load(
            context,
            adUnitIdFor(placement),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingPlacements.remove(placement)
                    loadedAds[placement] = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingPlacements.remove(placement)
                }
            }
        )
    }

    fun show(activity: Activity, placement: RewardPlacement, onResult: (RewardedAdResult) -> Unit) {
        val ad = loadedAds[placement]
        if (ad == null) {
            preload(placement)
            onResult(RewardedAdResult.NotReady)
            return
        }
        loadedAds.remove(placement)

        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                bgmPlayer.resumeAfterAd()
                preload(placement)
                onResult(if (earned) RewardedAdResult.Earned else RewardedAdResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                bgmPlayer.resumeAfterAd()
                preload(placement)
                onResult(RewardedAdResult.Failed(error.message))
            }
        }
        // 광고 영상 소리와 BGM이 오디오 포커스를 두고 다투지 않게 광고 동안은 BGM을 확실히 멈춘다
        bgmPlayer.pauseForAd()
        ad.show(activity) { earned = true }
    }
}
