package com.doge.simulator.ads

import android.app.Activity
import android.content.Context
import com.doge.simulator.BuildConfig
import com.doge.simulator.audio.BgmPlayer
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InterstitialAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bgmPlayer: BgmPlayer
) {
    private var loadedAd: InterstitialAd? = null
    private var isLoading = false

    fun preload() {
        if (loadedAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_UNIT_INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    loadedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                }
            }
        )
    }

    // shown: 광고가 실제로 노출됐는지 — 준비 안 됐거나 표시 실패면 false라 호출부가 노출 기록을 남기지 않는다
    fun show(activity: Activity, onDismiss: (shown: Boolean) -> Unit) {
        val ad = loadedAd
        if (ad == null) {
            preload()
            onDismiss(false)
            return
        }
        loadedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                bgmPlayer.resumeAfterAd()
                preload()
                onDismiss(true)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                bgmPlayer.resumeAfterAd()
                preload()
                onDismiss(false)
            }
        }
        // 광고 영상 소리와 BGM이 오디오 포커스를 두고 다투지 않게 광고 동안은 BGM을 확실히 멈춘다
        bgmPlayer.pauseForAd()
        ad.show(activity)
    }
}
