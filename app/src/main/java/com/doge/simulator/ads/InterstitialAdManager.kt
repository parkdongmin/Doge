package com.doge.simulator.ads

import android.app.Activity
import android.content.Context
import com.doge.simulator.BuildConfig
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
    @ApplicationContext private val context: Context
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

    fun show(activity: Activity, onDismiss: () -> Unit) {
        val ad = loadedAd
        if (ad == null) {
            preload()
            onDismiss()
            return
        }
        loadedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preload()
                onDismiss()
            }
        }
        ad.show(activity)
    }
}
