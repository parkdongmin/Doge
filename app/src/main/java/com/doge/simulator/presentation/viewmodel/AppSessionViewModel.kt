package com.doge.simulator.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.ads.RewardPlacement
import com.doge.simulator.ads.RewardedAdManager
import com.doge.simulator.ads.RewardedAdResult
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.usecase.CollectProfitUseCase
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.domain.usecase.PeekPendingProfitUseCase
import com.doge.simulator.domain.usecase.PendingProfit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// MainScreen 수명 동안 한 번 생성되어 앱 세션 전반(탭을 넘나드는) 상태를 관리한다.
// 오프라인 수익 확인 다이얼로그가 대표적 — 특정 탭의 화면에 종속시키면 다른 탭을 보는 동안엔 트리거될 수 없다
@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val peekPendingProfitUseCase: PeekPendingProfitUseCase,
    private val collectProfitUseCase: CollectProfitUseCase,
    private val getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val rewardedAdManager: RewardedAdManager
) : ViewModel() {

    private val _pendingOfflineProfit = MutableStateFlow<PendingProfit?>(null)
    val pendingOfflineProfit: StateFlow<PendingProfit?> = _pendingOfflineProfit.asStateFlow()

    init {
        rewardedAdManager.preload(RewardPlacement.OFFLINE_PROFIT_X2)
    }

    // ON_RESUME마다 호출 — 프로세스 최초 기동 시에도 발생하므로 별도의 초기 호출은 필요 없다
    fun checkPendingProfit() {
        if (_pendingOfflineProfit.value != null) return
        viewModelScope.launch {
            val planets = getOwnedPlanetsUseCase().first()
            if (planets.isEmpty()) return@launch
            val pending = peekPendingProfitUseCase(planets)
            if (pending.coins > 0 && pending.maxElapsedMinutes >= GameConstants.OFFLINE_PROFIT_DIALOG_THRESHOLD_MINUTES) {
                _pendingOfflineProfit.value = pending
            } else if (pending.coins > 0) {
                collectProfitUseCase(planets)
            }
        }
    }

    fun claimWithAd(activity: Activity) {
        rewardedAdManager.show(activity, RewardPlacement.OFFLINE_PROFIT_X2) { result ->
            val multiplier = if (result is RewardedAdResult.Earned) GameConstants.OFFLINE_PROFIT_AD_MULTIPLIER else 1.0
            claim(multiplier)
        }
    }

    fun claimFree() = claim(1.0)

    private fun claim(multiplier: Double) {
        viewModelScope.launch {
            val planets = getOwnedPlanetsUseCase().first()
            collectProfitUseCase(planets, multiplier)
            _pendingOfflineProfit.value = null
        }
    }
}
