package com.doge.simulator.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.R
import com.doge.simulator.ads.RewardPlacement
import com.doge.simulator.ads.RewardedAdManager
import com.doge.simulator.ads.RewardedAdResult
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.GetOwnedPlanetsUseCase
import com.doge.simulator.domain.usecase.GetResourcesUseCase
import com.doge.simulator.domain.usecase.SellPlanetUseCase
import com.doge.simulator.domain.usecase.UndoPlanetUpgradeUseCase
import com.doge.simulator.domain.usecase.UpgradePlanetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UndoableUpgradeFailure(val planetId: String, val previousLevel: Int, val investment: Long)

enum class UpgradeMessageTone { SUCCESS, FAIL, INFO }

data class UpgradeMessage(val text: String, val tone: UpgradeMessageTone, val iconRes: Int?)

// 강화 시도의 연출 단계. 코인/자원 부족·최대 레벨 같은 검증 실패는 도박이 아니므로 이 단계를
// 거치지 않고 바로 upgradeMessage로 즉시 표시하고, 실제 성공/실패 롤이 일어난 경우에만
// Charging(긴장 조성) → Revealing(결과 공개) 순서로 연출한다
sealed class UpgradePhase {
    object Idle : UpgradePhase()
    data class Charging(val isDangerZone: Boolean) : UpgradePhase()
    data class Revealing(val message: UpgradeMessage, val isDangerFail: Boolean) : UpgradePhase()
}

@HiltViewModel
class PlanetViewModel @Inject constructor(
    getOwnedPlanetsUseCase: GetOwnedPlanetsUseCase,
    private val sellPlanetUseCase: SellPlanetUseCase,
    private val upgradePlanetUseCase: UpgradePlanetUseCase,
    private val undoPlanetUpgradeUseCase: UndoPlanetUpgradeUseCase,
    private val getResourcesUseCase: GetResourcesUseCase,
    private val userRepository: UserRepository,
    private val rewardedAdManager: RewardedAdManager
) : ViewModel() {

    init {
        rewardedAdManager.preload(RewardPlacement.UPGRADE_REVERT)
    }

    val planets = getOwnedPlanetsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredVariantIds = userRepository.getDiscoveredVariantIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val resources = getResourcesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _upgradeMessage = MutableStateFlow<UpgradeMessage?>(null)
    val upgradeMessage: StateFlow<UpgradeMessage?> = _upgradeMessage.asStateFlow()

    private val _undoableFailure = MutableStateFlow<UndoableUpgradeFailure?>(null)
    val undoableFailure: StateFlow<UndoableUpgradeFailure?> = _undoableFailure.asStateFlow()

    private val _upgradePhase = MutableStateFlow<UpgradePhase>(UpgradePhase.Idle)
    val upgradePhase: StateFlow<UpgradePhase> = _upgradePhase.asStateFlow()

    fun sellPlanet(planet: Planet) {
        viewModelScope.launch { sellPlanetUseCase(planet) }
    }

    fun upgradePlanet(planet: Planet) {
        viewModelScope.launch {
            // 새 강화 시도를 시작하면 이전 실패에 대한 되돌리기는 더 이상 유효하지 않음
            _undoableFailure.value = null

            val result = upgradePlanetUseCase(planet)
            // 코인/자원 부족·최대 레벨 검증 실패는 도박이 아니라 즉시 알려줘야 할 정보라
            // 긴장 연출 없이 바로 처리. 실제로 성공/실패 롤이 일어난 경우에만 연출한다
            val isRoll = result is UpgradePlanetUseCase.Result.Success ||
                    result is UpgradePlanetUseCase.Result.Failed

            if (isRoll) {
                val isDangerZone = planet.level >= GameConstants.DANGER_ZONE_START
                _upgradePhase.value = UpgradePhase.Charging(isDangerZone)
                delay(if (isDangerZone) CHARGE_DURATION_DANGER_MS else CHARGE_DURATION_NORMAL_MS)
            }

            val (msg, isDangerFail) = when (result) {
                is UpgradePlanetUseCase.Result.Success ->
                    UpgradeMessage("강화 성공! Lv.${result.newLevel}", UpgradeMessageTone.SUCCESS, R.drawable.ic_ui_levelup) to false
                is UpgradePlanetUseCase.Result.Failed -> {
                    if (result.levelDropped) {
                        _undoableFailure.value = UndoableUpgradeFailure(planet.id, result.previousLevel, result.investment)
                    }
                    val text = if (result.levelDropped) "강화 실패 — Lv.${result.currentLevel}으로 하락"
                        else "강화 실패 — 레벨 유지"
                    UpgradeMessage(text, UpgradeMessageTone.FAIL, R.drawable.ic_ui_setback) to result.levelDropped
                }
                UpgradePlanetUseCase.Result.MaxLevel ->
                    UpgradeMessage("이미 최대 레벨입니다", UpgradeMessageTone.INFO, null) to false
                UpgradePlanetUseCase.Result.InsufficientCoins ->
                    UpgradeMessage("코인이 부족합니다", UpgradeMessageTone.INFO, null) to false
                UpgradePlanetUseCase.Result.InsufficientResources ->
                    UpgradeMessage("자원이 부족합니다", UpgradeMessageTone.INFO, null) to false
            }

            if (isRoll) {
                // 결과 공개 후엔 자동으로 사라지지 않고, 사용자가 확인/닫기를 눌러야 사라진다 —
                // 타이머로 사라지면 결과를 놓치거나, 되돌리기 버튼을 누를 새도 없이 없어질 수 있다
                _upgradePhase.value = UpgradePhase.Revealing(msg, isDangerFail)
            } else {
                _upgradeMessage.value = msg
                delay(3000)
                _upgradeMessage.value = null
            }
        }
    }

    // 강화 결과를 확인했을 때(결과 카드의 "확인"/다이얼로그 닫기) 호출 — 다음 강화를 바로
    // 시도할 수 있는 상태로 되돌린다
    fun dismissUpgradeResult() {
        _upgradePhase.value = UpgradePhase.Idle
    }

    companion object {
        // 안전구간 강화는 짧게, 위험구간(실패 시 레벨 하락)은 더 길고 긴장감 있게 충전
        private const val CHARGE_DURATION_NORMAL_MS = 900L
        private const val CHARGE_DURATION_DANGER_MS = 1800L
    }

    fun undoFailedUpgrade(activity: Activity) {
        val failure = _undoableFailure.value ?: return
        rewardedAdManager.show(activity, RewardPlacement.UPGRADE_REVERT) { result ->
            viewModelScope.launch {
                if (result is RewardedAdResult.Earned) {
                    undoPlanetUpgradeUseCase(failure.planetId, failure.previousLevel, failure.investment)
                    _undoableFailure.value = null
                    _upgradeMessage.value = UpgradeMessage(
                        "레벨이 복구되었습니다! Lv.${failure.previousLevel}", UpgradeMessageTone.SUCCESS, R.drawable.ic_ui_rewind
                    )
                    delay(3000)
                    _upgradeMessage.value = null
                } else {
                    _upgradeMessage.value = UpgradeMessage("광고를 끝까지 시청해야 되돌릴 수 있어요", UpgradeMessageTone.INFO, null)
                    delay(3000)
                    _upgradeMessage.value = null
                }
            }
        }
    }
}
