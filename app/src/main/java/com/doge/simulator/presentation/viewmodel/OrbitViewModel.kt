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
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import com.doge.simulator.domain.model.orbit.OrbitRoundState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

// Compose/StateFlow에 노출하는 불변 스냅샷. OrbitMatchState/OrbitRoundState는 도메인 계층에서
// 성능·테스트 편의를 위해 의도적으로 mutable하게 설계했기 때문에(같은 인스턴스를 계속 갱신),
// 그 참조를 StateFlow에 직접 넣으면 내용이 바뀌어도 동일 인스턴스라 새 값으로 인식되지 않아
// Compose가 재구성되지 않는다. 매 변경마다 이 데이터 클래스를 새로 만들어 발행한다.
//
// lastPlayerCard/lastB01Card: 각 진영이 "가장 최근에 낸 카드"만 보여주는 자리(그 이전
// 카드는 다음 카드가 나오는 순간 덮어써짐 — 전체 히스토리를 남기지 않는다는 FR-004는
// 지키면서도, 타이머로 사라지는 대신 다음 카드가 나올 때까지 계속 보여줘 읽을 시간을
// 확보한다). 라운드가 바뀌면 초기화된다.
data class OrbitUiSnapshot(
    val playerHand: List<OrbitCard>,
    val playerSignal: Int,
    val playerShielded: Boolean,
    val b01HandSize: Int,
    val b01Signal: Int,
    val b01Shielded: Boolean,
    val deckRemaining: Int,
    val currentTurn: PlayerSide,
    val roundFirstPlayer: PlayerSide,
    val matchOver: Boolean,
    val matchResult: MatchOutcome?,
    val bet: OrbitBet?,
    val lastPlayerCard: PlayOrbitCardUseCase.PlayedCardSummary? = null,
    val lastB01Card: PlayOrbitCardUseCase.PlayedCardSummary? = null,
    // 가장 최근에 놓인 카드(진영 무관)와 놓일 때마다 1씩 느는 번호 — 스포트라이트 연출용.
    val latestPlay: PlayOrbitCardUseCase.PlayedCardSummary? = null,
    val playSeq: Int = 0
)

// 라운드가 끝날 때마다(사유 불문) 채워진다. "방금 이 카드를 썼다"는 알아도 "그래서 라운드가
// 끝났다"는 게 눈에 안 띄고 SIGNAL만 조용히 바뀌어 지나가버린다는 피드백을 반영 — 라운드
// 종료는 항상 이유·승자·현재 SIGNAL 스코어를 명시적으로 보여주고 확인을 받는다.
data class OrbitRoundEndInfo(
    val reason: RoundEndReason,
    val winner: PlayerSide?,
    val playerSignal: Int,
    val b01Signal: Int,
    // 라운드가 끝난 순간 양쪽이 들고 있던 카드 — 라운드 종료 시 서로의 패를 공개한다. PROBE로 졌는데
    // "상대가 정말 더 높은 카드였나?" 확인할 방법이 없다는 피드백 반영.
    val playerCard: OrbitCard? = null,
    val b01Card: OrbitCard? = null
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
        roundFirstPlayer = round.firstPlayerOfRound,
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

    // 휴게실의 베팅 모달 표시 여부. 결과 화면 "다시 하기"로 휴게실에 돌아왔을 때 모달이 바로
    // 열려 있어야 해서 화면 로컬 상태가 아니라 여기(휴게실 엔트리 스코프 뷰모델)에 둔다.
    private val _betDialogVisible = MutableStateFlow(false)
    val betDialogVisible: StateFlow<Boolean> = _betDialogVisible.asStateFlow()

    // 카드가 놓일 때마다 true — 게임 화면이 그 카드의 스포트라이트 연출을 다 보여주고 나면
    // onPlayPresented()로 false로 돌려놓는다. B-01은 이게 false가 될 때까지 다음 수를 두지 않는다.
    private val _presentingPlay = MutableStateFlow(false)

    fun onPlayPresented() {
        _presentingPlay.value = false
    }

    private val _uiSnapshot = MutableStateFlow<OrbitUiSnapshot?>(null)
    val uiSnapshot: StateFlow<OrbitUiSnapshot?> = _uiSnapshot.asStateFlow()

    // 라운드가 끝날 때마다 채워진다(사유 불문) — 이때만 명시적으로 "확인"을 받는다.
    // 카드 한 장 한 장의 결과는 이제 uiSnapshot의 lastPlayerCard/lastB01Card로 계속
    // 보여주므로(타이머도 확인 버튼도 없음) 별도로 막지 않는다.
    private val _roundEndBanner = MutableStateFlow<OrbitRoundEndInfo?>(null)
    val roundEndBanner: StateFlow<OrbitRoundEndInfo?> = _roundEndBanner.asStateFlow()

    private val _lastSettlement = MutableStateFlow<OrbitBetSettlement?>(null)
    val lastSettlement: StateFlow<OrbitBetSettlement?> = _lastSettlement.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // 현재 진행 중인 매치의 실제 가변 도메인 상태. UI는 이걸 직접 보지 않고 uiSnapshot만 본다.
    private var activeMatch: OrbitMatchState? = null

    // "마지막으로 낸 카드" 슬롯 — 다음 카드가 나오면 덮어써진다(전체 로그 아님, FR-004).
    private var lastPlayerPlay: PlayOrbitCardUseCase.PlayedCardSummary? = null
    private var lastB01Play: PlayOrbitCardUseCase.PlayedCardSummary? = null

    // 가장 최근에 놓인 카드와 그 일련번호 — 화면의 스포트라이트 연출이 "새 카드가 놓였다"를 알아채는
    // 기준. 같은 결과의 카드를 연달아 내면(SCOUT DRONE 연속 빗나감 등) 요약 값이 똑같아 값 비교로는
    // 구분이 안 되므로 번호로 구분한다.
    private var latestPlay: PlayOrbitCardUseCase.PlayedCardSummary? = null
    private var playSeq = 0

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

    fun openBetDialog() {
        _betDialogVisible.value = true
    }

    fun closeBetDialog() {
        _betDialogVisible.value = false
        _presentingPlay.value = false
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
        lastPlayerPlay = null
        lastB01Play = null
        latestPlay = null
        _lastSettlement.value = null
        _roundEndBanner.value = null
        activeMatch = match
        _betDialogVisible.value = false
        _presentingPlay.value = false
        publishSnapshot()
        viewModelScope.launch { advanceUntilPlayerTurnOrPause() }
    }

    fun playCard(card: OrbitCard, input: OrbitCardEffectInput = OrbitCardEffectInput.None) {
        val match = activeMatch ?: return
        val round = match.currentRound
        // 라운드 종료 배너를 아직 확인 안 했으면 입력을 받지 않는다.
        if (round.currentTurn != PlayerSide.PLAYER || round.isOver || _roundEndBanner.value != null) return

        val result = playOrbitCardUseCase(round, PlayerSide.PLAYER, card, input, b01Memory)
        if (result is PlayOrbitCardUseCase.Result.Applied) {
            recordPlay(result.summary)
            finishRoundIfNeeded(match)
            publishSnapshot()
            if (_roundEndBanner.value == null) {
                viewModelScope.launch { advanceUntilPlayerTurnOrPause() }
            }
            // 라운드가 끝났다면 배너가 떴으니 acknowledgeAndContinue()가 이어서 진행한다.
        } else {
            // InvalidMove면 상태를 전혀 바꾸지 않는다 — 대신 왜 안 되는지 알려준다. 예전엔
            // 아무 설명 없이 그냥 아무 일도 안 일어나서(예: AI_CORE 강제 사용 규칙에 걸림)
            // 플레이어가 원인을 알 수 없었다는 피드백을 반영.
            viewModelScope.launch { showMessage(invalidMoveMessage(round, card)) }
        }
    }

    private fun invalidMoveMessage(round: OrbitRoundState, card: OrbitCard): String {
        val hand = round.player(PlayerSide.PLAYER).hand
        val mustPlayAiCore = hand.any { it.type == OrbitCardType.AI_CORE } &&
            hand.any { it.type == OrbitCardType.EMP || it.type == OrbitCardType.WARP_GATE }
        return when {
            mustPlayAiCore && card.type != OrbitCardType.AI_CORE ->
                "AI CORE와 EMP/WARP GATE를 같이 들고 있으면 AI CORE부터 내야 해요"
            card.type == OrbitCardType.EMP && round.deck.remainingCount < 1 ->
                "덱에 카드가 없어서 EMP를 쓸 수 없어요"
            else -> "지금은 낼 수 없는 카드예요"
        }
    }

    // 라운드 종료 배너를 확인하고 다음 라운드/결과 화면으로 진행한다.
    fun acknowledgeAndContinue() {
        if (_roundEndBanner.value == null) return
        _roundEndBanner.value = null
        lastPlayerPlay = null
        lastB01Play = null
        latestPlay = null
        val match = activeMatch ?: return
        if (match.isOver) {
            publishSnapshot() // 결과 화면 이동은 matchOver 관찰로 처리됨(화면 쪽 LaunchedEffect)
            return
        }
        // 다음 라운드는 배너를 확인한 "지금" 시작한다 — onRoundEnded() 시점에 바로 시작해버리면
        // 아직 방금 끝난 라운드 결과를 보여주는 배너가 떠 있는 동안에도 화면에 이미 다음
        // 라운드의 새 덱 장수·새 손패가 섞여 보이는 문제가 있었다(실기기 리포트).
        match.startNextRoundIfNotOver()
        publishSnapshot()
        viewModelScope.launch { advanceUntilPlayerTurnOrPause() }
    }

    fun leaveMatch() {
        val match = activeMatch ?: return
        settleOrbitBetUseCase.settleAbandoned(match)
        activeMatch = null
        _uiSnapshot.value = null
        _roundEndBanner.value = null
        lastPlayerPlay = null
        lastB01Play = null
        latestPlay = null
    }

    fun playAgain() {
        _lastSettlement.value = null
        activeMatch = null
        _uiSnapshot.value = null
        _roundEndBanner.value = null
        lastPlayerPlay = null
        lastB01Play = null
        latestPlay = null
        // 휴게실로 돌아가면서 베팅 모달을 바로 연다(예전 "베팅 화면으로 이동"과 같은 흐름).
        _betDialogVisible.value = true
    }

    fun returnToLounge() {
        _lastSettlement.value = null
        activeMatch = null
        _uiSnapshot.value = null
        _roundEndBanner.value = null
        lastPlayerPlay = null
        lastB01Play = null
        latestPlay = null
    }

    // 내 턴이 될 때까지 B-01의 턴을 연달아 진행한다(매 수마다 uiSnapshot의 lastB01Card가
    // 갱신되어 화면에 계속 보임 — 확인 없이도 다음 수로 자연스럽게 넘어감). 라운드가 끝나면
    // 그 즉시 종료 배너를 띄우고 멈춘다 — acknowledgeAndContinue()가 이어서 진행한다.
    private suspend fun advanceUntilPlayerTurnOrPause() {
        var match = activeMatch ?: return
        if (match.isOver || _roundEndBanner.value != null) return

        while (!match.isOver && !match.currentRound.isOver && match.currentRound.currentTurn == PlayerSide.B01) {
            // 방금 놓인 카드(주로 내 카드)의 스포트라이트 연출이 끝날 때까지 B-01은 기다린다 — 안 그러면
            // 내 카드가 아직 가운데 떠 있는데 그 뒤로 B-01이 이미 카드를 내버려 어색했다.
            // 화면이 신호를 못 주는 경우(화면 이탈 등)에 영영 멈추지 않도록 상한을 둔다.
            withTimeoutOrNull(PRESENTATION_WAIT_TIMEOUT_MS) { _presentingPlay.first { !it } }
            if (activeMatch !== match) return // 기다리는 사이 매치를 나갔거나 새 매치가 시작됨
            if (match.isOver || match.currentRound.isOver) break
            val canPlay = advanceOrbitTurnUseCase(match.currentRound)
            if (!canPlay) {
                finishRoundIfNeeded(match)
                publishSnapshot()
                return
            }
            publishSnapshot()
            delay(B01_THINK_DELAY_MS)

            val mistakeRate = match.bet?.riskTier?.aiMistakeRate ?: OrbitRiskTier.LOWEST.aiMistakeRate
            val decision = B01OrbitAi.decide(match.currentRound, b01Memory, mistakeRate)
            val result = playOrbitCardUseCase(
                match.currentRound, PlayerSide.B01, decision.card, decision.input, b01Memory
            )
            if (result is PlayOrbitCardUseCase.Result.Applied) {
                recordPlay(result.summary)
                finishRoundIfNeeded(match)
                publishSnapshot()
                if (_roundEndBanner.value != null || match.isOver) return
            } else {
                // 이론상 발생하면 안 됨 — B01OrbitAi는 낼 수 없는 카드를 애초에 고르지 않는다.
                // 혹시 발생하더라도 무한 반복하지 않고 여기서 조용히 멈춘다.
                publishSnapshot()
                return
            }
            match = activeMatch ?: return
        }

        // 사람 턴이면 드로우까지만 미리 진행해 손패 2장을 보여준다.
        if (!match.isOver && !match.currentRound.isOver && match.currentRound.currentTurn == PlayerSide.PLAYER) {
            val canPlay = advanceOrbitTurnUseCase(match.currentRound)
            if (!canPlay) finishRoundIfNeeded(match)
            publishSnapshot()
        }
    }

    private fun recordPlay(summary: PlayOrbitCardUseCase.PlayedCardSummary) {
        _presentingPlay.value = true
        playSeq++
        latestPlay = summary
        if (summary.by == PlayerSide.PLAYER) lastPlayerPlay = summary else lastB01Play = summary
    }

    // 라운드가 끝나면 사유와 관계없이 항상 종료 배너를 채운다. 매치가 함께 끝났다면 베팅도 정산한다.
    private fun finishRoundIfNeeded(match: OrbitMatchState) {
        if (!match.currentRound.isOver) return
        val endedRound = match.currentRound
        match.onRoundEnded()
        _roundEndBanner.value = OrbitRoundEndInfo(
            reason = endedRound.endReason!!,
            winner = endedRound.winner,
            playerSignal = match.signals[PlayerSide.PLAYER] ?: 0,
            b01Signal = match.signals[PlayerSide.B01] ?: 0,
            playerCard = revealedCardAtRoundEnd(endedRound, PlayerSide.PLAYER),
            b01Card = revealedCardAtRoundEnd(endedRound, PlayerSide.B01)
        )
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

    // 라운드 종료 때 공개할 한쪽의 카드: 손에 남은 카드, 없으면(EMP로 CAPTAIN이 버려져 OUT된 경우)
    // 방금 버려진 그 카드.
    private fun revealedCardAtRoundEnd(round: OrbitRoundState, side: PlayerSide): OrbitCard? =
        round.player(side).hand.firstOrNull()
            ?: latestPlay?.takeIf { it.outSide == side }?.discardedCard

    private fun publishSnapshot() {
        _uiSnapshot.value = activeMatch?.toSnapshot()?.copy(
            lastPlayerCard = lastPlayerPlay,
            lastB01Card = lastB01Play,
            latestPlay = latestPlay,
            playSeq = playSeq
        )
    }

    private suspend fun showMessage(msg: String) {
        _message.value = msg
        delay(2500)
        _message.value = null
    }

    private companion object {
        const val B01_THINK_DELAY_MS = 600L
        private const val PRESENTATION_WAIT_TIMEOUT_MS = 5_000L
    }
}
