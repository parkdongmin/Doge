package com.doge.simulator.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.ads.OrbitDailyRewardGate
import com.doge.simulator.ads.RewardPlacement
import com.doge.simulator.ads.RewardedAdManager
import com.doge.simulator.ads.RewardedAdResult
import com.doge.simulator.domain.ai.B01Memory
import com.doge.simulator.domain.ai.B01OrbitAi
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitBet
import com.doge.simulator.domain.model.orbit.OrbitBetSettlement
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.repository.UserRepository
import com.doge.simulator.domain.usecase.orbit.AdvanceOrbitTurnUseCase
import com.doge.simulator.domain.usecase.orbit.ClaimOrbitDailyAdRewardUseCase
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase
import com.doge.simulator.domain.usecase.orbit.SettleOrbitBetUseCase
import com.doge.simulator.domain.usecase.orbit.StartOrbitMatchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Compose/StateFlow에 노출하는 불변 스냅샷. OrbitMatchState/OrbitRoundState는 도메인 계층에서
// 성능·테스트 편의를 위해 의도적으로 mutable하게 설계했기 때문에(같은 인스턴스를 계속 갱신),
// 그 참조를 StateFlow에 직접 넣으면 내용이 바뀌어도 동일 인스턴스라 새 값으로 인식되지 않아
// Compose가 재구성되지 않는다. 매 변경마다 이 데이터 클래스를 새로 만들어 발행한다.
data class OrbitUiSnapshot(
    val playerHand: List<OrbitCard>,
    val playerSignal: Int,
    val playerShielded: Boolean,
    val b01HandSize: Int,
    val b01Signal: Int,
    val b01Shielded: Boolean,
    val deckRemaining: Int,
    val currentTurn: PlayerSide,
    val matchOver: Boolean,
    val matchResult: MatchOutcome?,
    val bet: OrbitBet?
)

private fun OrbitMatchState.toSnapshot(): OrbitUiSnapshot {
    val round = currentRound
    return OrbitUiSnapshot(
        playerHand = round.player(PlayerSide.PLAYER).hand.toList(),
        playerSignal = signals[PlayerSide.PLAYER] ?: 0,
        playerShielded = round.player(PlayerSide.PLAYER).shieldActive,
        b01HandSize = round.player(PlayerSide.B01).hand.size,
        b01Signal = signals[PlayerSide.B01] ?: 0,
        b01Shielded = round.player(PlayerSide.B01).shieldActive,
        deckRemaining = round.deck.remainingCount,
        currentTurn = round.currentTurn,
        matchOver = isOver,
        matchResult = matchResult,
        bet = bet
    )
}

@HiltViewModel
class OrbitViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val startOrbitMatchUseCase: StartOrbitMatchUseCase,
    private val advanceOrbitTurnUseCase: AdvanceOrbitTurnUseCase,
    private val playOrbitCardUseCase: PlayOrbitCardUseCase,
    private val settleOrbitBetUseCase: SettleOrbitBetUseCase,
    private val claimOrbitDailyAdRewardUseCase: ClaimOrbitDailyAdRewardUseCase,
    private val orbitDailyRewardGate: OrbitDailyRewardGate,
    private val rewardedAdManager: RewardedAdManager
) : ViewModel() {

    val coins: StateFlow<Long> = userRepository.getCoins()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val betAmounts: List<Long> = GameConstants.ORBIT_BET_AMOUNTS
    val riskTiers: List<OrbitRiskTier> = OrbitRiskTier.entries

    private val _dailyAdRemaining = MutableStateFlow(0)
    val dailyAdRemaining: StateFlow<Int> = _dailyAdRemaining.asStateFlow()

    private val _selectedBetAmount = MutableStateFlow<Long?>(null)
    val selectedBetAmount: StateFlow<Long?> = _selectedBetAmount.asStateFlow()

    private val _selectedRiskTier = MutableStateFlow(OrbitRiskTier.LOWEST)
    val selectedRiskTier: StateFlow<OrbitRiskTier> = _selectedRiskTier.asStateFlow()

    private val _uiSnapshot = MutableStateFlow<OrbitUiSnapshot?>(null)
    val uiSnapshot: StateFlow<OrbitUiSnapshot?> = _uiSnapshot.asStateFlow()

    // "방금 사용된 카드"는 일회성으로만 노출한다 — 지속 로그는 만들지 않는다(FR-004).
    private val _lastPlayedCard = MutableStateFlow<PlayOrbitCardUseCase.PlayedCardSummary?>(null)
    val lastPlayedCard: StateFlow<PlayOrbitCardUseCase.PlayedCardSummary?> = _lastPlayedCard.asStateFlow()

    private val _lastSettlement = MutableStateFlow<OrbitBetSettlement?>(null)
    val lastSettlement: StateFlow<OrbitBetSettlement?> = _lastSettlement.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // 현재 진행 중인 매치의 실제 가변 도메인 상태. UI는 이걸 직접 보지 않고 uiSnapshot만 본다.
    private var activeMatch: OrbitMatchState? = null

    // B-01은 사람처럼 손패를 직접 확인하지 않는다(FR-010) — 이 인스턴스에만 확인/교환 정보가 쌓인다.
    private var b01Memory = B01Memory()

    init {
        refreshDailyAdRemaining()
        rewardedAdManager.preload(RewardPlacement.ORBIT_DAILY)
    }

    fun refreshDailyAdRemaining() {
        _dailyAdRemaining.value = orbitDailyRewardGate.remainingToday()
    }

    fun claimDailyAdReward(activity: Activity) {
        rewardedAdManager.show(activity, RewardPlacement.ORBIT_DAILY) { result ->
            viewModelScope.launch {
                if (result is RewardedAdResult.Earned) {
                    when (claimOrbitDailyAdRewardUseCase()) {
                        ClaimOrbitDailyAdRewardUseCase.Result.Granted -> {
                            refreshDailyAdRemaining()
                            showMessage("재화 ${GameConstants.ORBIT_DAILY_AD_REWARD_COINS}을 받았어요!")
                        }
                        ClaimOrbitDailyAdRewardUseCase.Result.LimitReached -> {
                            refreshDailyAdRemaining()
                            showMessage("오늘은 더 받을 수 없어요")
                        }
                    }
                }
                // Dismissed/Failed/NotReady: 아무 것도 하지 않는다(FR-026) — 재시도만 가능하면 됨
            }
        }
    }

    fun selectBetAmount(amount: Long) {
        _selectedBetAmount.value = amount
    }

    fun selectRiskTier(tier: OrbitRiskTier) {
        _selectedRiskTier.value = tier
    }

    fun startMatch() {
        val amount = _selectedBetAmount.value ?: return
        viewModelScope.launch {
            when (val result = startOrbitMatchUseCase(amount, _selectedRiskTier.value)) {
                is StartOrbitMatchUseCase.Result.Success -> beginMatch(result.matchState)
                StartOrbitMatchUseCase.Result.InsufficientCoins -> showMessage("재화가 부족해요")
            }
        }
    }

    // 베팅/정거장 연동 없이 카드 규칙만 단독으로 확인할 때 쓰는 진입점 (US1 독립 실행).
    fun startPracticeMatch() {
        beginMatch(OrbitMatchState.start())
    }

    private fun beginMatch(match: OrbitMatchState) {
        b01Memory = B01Memory()
        _lastSettlement.value = null
        activeMatch = match
        publishSnapshot()
        viewModelScope.launch { runTurnLoop() }
    }

    fun playCard(card: OrbitCard, input: OrbitCardEffectInput = OrbitCardEffectInput.None) {
        val match = activeMatch ?: return
        val round = match.currentRound
        if (round.currentTurn != PlayerSide.PLAYER || round.isOver) return

        val result = playOrbitCardUseCase(round, PlayerSide.PLAYER, card, input, b01Memory)
        if (result is PlayOrbitCardUseCase.Result.Applied) {
            emitPlayedCard(result.summary)
        }
        finishRoundIfNeeded(match)
        publishSnapshot()
        viewModelScope.launch { runTurnLoop() }
    }

    fun leaveMatch() {
        val match = activeMatch ?: return
        settleOrbitBetUseCase.settleAbandoned(match)
        activeMatch = null
        _uiSnapshot.value = null
    }

    fun playAgain() {
        _lastSettlement.value = null
        activeMatch = null
        _uiSnapshot.value = null
    }

    fun returnToLounge() {
        _lastSettlement.value = null
        activeMatch = null
        _uiSnapshot.value = null
    }

    // B-01 턴을 필요한 만큼 자동으로 진행한다. 사람 턴이 되거나 매치가 끝나면 멈춘다.
    private suspend fun runTurnLoop() {
        var match = activeMatch ?: return
        while (!match.isOver && !match.currentRound.isOver && match.currentRound.currentTurn == PlayerSide.B01) {
            val canPlay = advanceOrbitTurnUseCase(match.currentRound)
            if (!canPlay) {
                finishRoundIfNeeded(match)
                publishSnapshot()
                match = activeMatch ?: return
                continue
            }
            publishSnapshot()
            delay(B01_THINK_DELAY_MS)

            val mistakeRate = match.bet?.riskTier?.aiMistakeRate ?: OrbitRiskTier.LOWEST.aiMistakeRate
            val decision = B01OrbitAi.decide(match.currentRound, b01Memory, mistakeRate)
            val result = playOrbitCardUseCase(
                match.currentRound, PlayerSide.B01, decision.card, decision.input, b01Memory
            )
            if (result is PlayOrbitCardUseCase.Result.Applied) {
                emitPlayedCard(result.summary)
            }
            finishRoundIfNeeded(match)
            publishSnapshot()
            match = activeMatch ?: return
        }
        // 사람 턴이면 드로우까지만 미리 진행해 손패 2장을 보여준다.
        if (!match.isOver && !match.currentRound.isOver && match.currentRound.currentTurn == PlayerSide.PLAYER) {
            val canPlay = advanceOrbitTurnUseCase(match.currentRound)
            if (!canPlay) finishRoundIfNeeded(match)
            publishSnapshot()
        }
    }

    private fun finishRoundIfNeeded(match: OrbitMatchState) {
        if (!match.currentRound.isOver) return
        match.onRoundEnded()
        if (match.isOver) {
            viewModelScope.launch {
                _lastSettlement.value = settleOrbitBetUseCase.settleFinished(match)
                publishSnapshot()
                // 매치 종료 후 전면광고 훅 지점 — 정확한 노출 빈도(몇 판마다/조건부)는
                // spec.md Assumptions에 따라 이번 범위에서 확정하지 않는다. 실제로 노출하려면
                // 여기서 기존 InterstitialAdManager + AdFrequencyGate 조합을 재사용하면 된다.
            }
        }
    }

    private fun emitPlayedCard(summary: PlayOrbitCardUseCase.PlayedCardSummary) {
        _lastPlayedCard.value = summary
        viewModelScope.launch {
            delay(CARD_REVEAL_DURATION_MS)
            if (_lastPlayedCard.value == summary) _lastPlayedCard.value = null
        }
    }

    private fun publishSnapshot() {
        _uiSnapshot.value = activeMatch?.toSnapshot()
    }

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(2500)
        _message.value = null
    }

    private companion object {
        const val B01_THINK_DELAY_MS = 600L
        const val CARD_REVEAL_DURATION_MS = 1500L
    }
}
