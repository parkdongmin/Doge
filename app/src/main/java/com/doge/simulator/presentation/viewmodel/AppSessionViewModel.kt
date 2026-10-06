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
            val longElapsed = pending.maxElapsedMinutes >= GameConstants.OFFLINE_PROFIT_DIALOG_THRESHOLD_MINUTES
            when {
                // 이득이 오래 쌓였으면 "2배 보기" 팝업, 짧으면 조용히 자동 수집
                pending.coins > 0 && longElapsed -> _pendingOfflineProfit.value = pending
                pending.coins > 0 -> collectProfitUseCase(planets)
                // 손해도 오래 쌓였으면 알려주고 확인시키되(광고 배율 옵션 없음), 짧으면 조용히 반영
                pending.coins < 0 && longElapsed -> _pendingOfflineProfit.value = pending
                pending.coins < 0 -> collectProfitUseCase(planets)
            }
        }
    }

    // 2배 광고가 안 됐을 때 다이얼로그 안에 띄우는 안내. 예전엔 광고가 없거나 실패해도 조용히
    // 1배로 수령하고 닫아버려서, "광고 보고 받기"를 눌렀는데 1배만 들어오는 일이 생겼다 —
    // 이제는 다이얼로그를 그대로 두고 다시 시도하거나 그냥 받기를 고르게 한다.
    private val _offlineAdNotice = MutableStateFlow<String?>(null)
    val offlineAdNotice: StateFlow<String?> = _offlineAdNotice.asStateFlow()

    fun claimWithAd(activity: Activity) {
        rewardedAdManager.show(activity, RewardPlacement.OFFLINE_PROFIT_X2) { result ->
            when (result) {
                RewardedAdResult.Earned -> claim(GameConstants.OFFLINE_PROFIT_AD_MULTIPLIER)
                RewardedAdResult.Dismissed ->
                    _offlineAdNotice.value = "광고를 끝까지 봐야 2배로 받을 수 있어요"
                RewardedAdResult.NotReady, is RewardedAdResult.Failed ->
                    _offlineAdNotice.value = "광고를 불러오지 못했어요. 잠시 후 다시 눌러 주세요"
            }
        }
    }

    fun claimFree() = claim(1.0)

    private fun claim(multiplier: Double) {
        viewModelScope.launch {
            val planets = getOwnedPlanetsUseCase().first()
            collectProfitUseCase(planets, multiplier)
            _pendingOfflineProfit.value = null
            _offlineAdNotice.value = null
        }
    }
}
