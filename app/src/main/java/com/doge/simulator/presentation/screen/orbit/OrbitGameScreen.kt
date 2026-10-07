package com.doge.simulator.presentation.screen.orbit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase
import com.doge.simulator.presentation.component.NightSkyBackground
import com.doge.simulator.presentation.component.GameButton
import com.doge.simulator.presentation.component.GameButtonSize
import com.doge.simulator.presentation.component.GameButtonStyle
import com.doge.simulator.presentation.component.GameDialog
import com.doge.simulator.presentation.component.pixelFrame
import com.doge.simulator.presentation.component.pixelShape
import com.doge.simulator.presentation.viewmodel.OrbitRoundEndInfo
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*
import kotlinx.coroutines.delay

// ORBIT 게임 화면.
//
// 레이아웃: 위쪽 B-01(아바타+말풍선), 가운데 "테이블"(양 진영이 마지막으로 낸 카드 + 덱 더미),
// 아래쪽 플레이어 바와 손패.
//
// 카드 사용 결과는 테이블 위 "마지막으로 낸 카드" 자리(진영당 하나)에 계속 남아있다가 그 진영이
// 다음 카드를 내는 순간 덮어써진다 — 타이머나 확인 버튼 없이도 방금 무슨 일이 있었는지 내
// 페이스로 읽을 수 있고, 그 이전 기록까지는 남지 않아 전체 히스토리를 만들지는 않는다(FR-004).
// 라운드가 끝나는 것만은 별도로 명시적인 "확인"을 받는다 — SIGNAL만 조용히 바뀌고 지나가면
// 라운드가 끝난 건지 헷갈린다는 피드백 반영.
//
// 카드 선택은 2단계다: 손패에서 카드를 누르면 먼저 "선택"만 되어 카드가 살짝 들려 올라오고 손패 위
// 선택 줄에 설명 + [사용]이 뜬다(바로 발동되지 않음) — [사용]을 눌러야 실제로 낸다. 실수로 CAPTAIN을
// 눌러 즉시 OUT되는 사고 등을 막기 위함. 카드가 실제로 놓이는 순간(나·B-01 모두)엔 화면이 어두워지며
// 그 카드가 가운데로 튀어나오는 스포트라이트 연출이 잠깐 뜬다(자동으로 닫힘, 누르면 바로 넘김).
@Composable
fun OrbitGameScreen(
    onExit: () -> Unit,
    onMatchFinished: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val snapshot by viewModel.uiSnapshot.collectAsState()
    val roundEndBanner by viewModel.roundEndBanner.collectAsState()
    val message by viewModel.message.collectAsState()
    // 카드 값이 아니라 손패 안에서의 "자리(인덱스)"로 선택을 추적한다 — OrbitCard는 종류만으로
    // 동등성을 따지는 data class라, 같은 카드 2장(SENSOR 2장 등)을 들고 있을 때 값으로 비교하면
    // 하나를 눌러도 두 장 다 선택된 것처럼(테두리가 둘 다 표시) 보이는 버그가 있었다.
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var pendingScoutCard by remember { mutableStateOf<OrbitCard?>(null) }
    var pendingEmpCard by remember { mutableStateOf<OrbitCard?>(null) }

    // 매치가 방금 끝났더라도, 라운드 종료 배너를 플레이어가 아직 확인하지 않았다면 곧바로
    // 결과 화면으로 넘기지 않는다 — 안 그러면 승부를 가른 장면을 볼 새도 없이 화면이
    // 바뀌어버린다.
    LaunchedEffect(snapshot?.matchOver, roundEndBanner) {
        if (snapshot?.matchOver == true && roundEndBanner == null) onMatchFinished()
    }

    // 라운드 종료 확인 대기 중이거나 내 턴이 아니게 되면 선택을 풀어준다.
    LaunchedEffect(snapshot?.currentTurn, roundEndBanner) {
        if (roundEndBanner != null || snapshot?.currentTurn != PlayerSide.PLAYER) selectedIndex = null
    }

    // 시스템/제스처 뒤로가기가 "나가기" 버튼을 그냥 지나쳐 화면만 닫아버리면 activeMatch가
    // 정리되지 않은 채 남는다 — 그 상태로 휴게실에서 다시 카드 테이블에 들어가면 베팅 화면이
    // 남아있던 매치 스냅샷을 보고 곧장 게임 화면으로 튀어버리는 버그로 이어졌다. 뒤로가기도
    // 반드시 leaveMatch()를 거치게 한다.
    // 도중 이탈은 즉시 패배(베팅 코인 손실)라, 뒤로가기·"나가기" 모두 바로 나가지 않고 확인을 받는다 —
    // 제스처 내비게이션에선 가장자리를 실수로 쓸기만 해도 뒤로가기가 돼서 한 번에 코인을 잃을 수 있었다.
    // 매치가 이미 끝났으면(마지막 라운드 배너 대기 중) 잃을 게 없으니 확인 없이 나간다.
    var showLeaveConfirm by remember { mutableStateOf(false) }
    fun requestLeave() {
        if (snapshot?.matchOver == true) {
            viewModel.leaveMatch()
            onExit()
        } else {
            showLeaveConfirm = true
        }
    }
    BackHandler { requestLeave() }

    val current = snapshot ?: return
    val canAct = current.currentTurn == PlayerSide.PLAYER && roundEndBanner == null

    fun confirmPlay(card: OrbitCard) {
        when (card.type) {
            OrbitCardType.SCOUT_DRONE -> pendingScoutCard = card
            OrbitCardType.EMP -> pendingEmpCard = card
            else -> {
                viewModel.playCard(card)
                selectedIndex = null
            }
        }
    }

    // B-01 말풍선 대사 — 상황(라운드 종료/누구 차례/방금 낸 카드)이 바뀔 때만 새로 뽑는다.
    // 같은 스냅샷이 다시 발행돼도(드로우 등) 키가 같으면 대사가 바뀌지 않는다.
    val b01Line = remember(
        roundEndBanner, current.currentTurn, current.lastPlayerCard, current.lastB01Card, current.roundFirstPlayer
    ) {
        val banner = roundEndBanner
        val lastB01 = current.lastB01Card
        val pool = when {
            banner != null -> B01Lines.roundEnd(banner)
            current.currentTurn == PlayerSide.B01 ->
                current.lastPlayerCard?.let(B01Lines::afterPlayerPlay) ?: B01Lines.thinking
            lastB01 != null -> B01Lines.afterOwnPlay(lastB01)
            else -> B01Lines.roundStart(current.roundFirstPlayer == PlayerSide.PLAYER)
        }
        pool.random()
    }

    // 카드가 놓이는 순간의 "스포트라이트" 연출 대기열. 내 카드와 B-01 카드가 거의 연달아 나오므로
    // (B-01은 600ms 생각 후 바로 냄) 겹치지 않게 순서대로 하나씩 보여준다.
    val spotlightQueue = remember { mutableStateListOf<PlayOrbitCardUseCase.PlayedCardSummary>() }
    var spotlight by remember { mutableStateOf<PlayOrbitCardUseCase.PlayedCardSummary?>(null) }
    LaunchedEffect(current.playSeq) { current.latestPlay?.let { spotlightQueue.add(it) } }
    LaunchedEffect(spotlight, spotlightQueue.size) {
        if (spotlight == null) {
            if (spotlightQueue.isNotEmpty()) spotlight = spotlightQueue.removeAt(0)
            // 보여줄 연출이 다 끝났다 — B-01이 다음 수를 둬도 된다고 알린다.
            else viewModel.onPlayPresented()
        }
    }
    // (a) 방식: 잠깐 보여주고 자동으로 닫힌다(화면을 누르면 바로 넘김). 결과는 테이블의
    // "낸 카드" 자리에 계속 남으므로 연출이 닫혀도 정보를 놓치지 않는다.
    LaunchedEffect(spotlight) {
        if (spotlight != null) {
            delay(SPOTLIGHT_DURATION_MS)
            spotlight = null
        }
    }

    // 넓은 화면(폴드 펼침·태블릿)에선 게임 영역 폭을 제한해 가운데 정렬하고, 세로가 짧은
    // 화면에선 손패 카드를 한 단계 작게 — 어떤 화면에서도 테이블이 잘리지 않게 한다.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < COMPACT_HEIGHT
        val handCardSize = if (compact) OrbitCardSize.COMPACT else OrbitCardSize.NORMAL
        // 휴게실·결과 화면과 같은 밤하늘(반짝이는 별) 배경.
        NightSkyBackground(Modifier.matchParentSize()) {}
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MAX_GAME_WIDTH)
                .fillMaxSize()
                .statusBarsPadding()
                // 하단 내비게이션 바(3버튼·제스처 모두) 영역만큼 띄워 손패가 가려지지 않게 — 인셋 크기는
                // 기기·설정에 따라 시스템이 알려주는 값을 그대로 쓴다.
                .navigationBarsPadding()
                .padding(Spacing.lg)
        ) {
            // 상단: 타이틀 / 베팅 / 나가기
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ORBIT", color = GoldAccent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    current.bet?.let {
                        Text("BET ${it.amount}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(Spacing.sm))
                    }
                    GameButton(text = "나가기", onClick = { requestLeave() }, style = GameButtonStyle.Danger)
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // 위쪽: B-01 아바타 + 말풍선, 그 아래 손패 뒷면·쉴드·SIGNAL
            B01SpeechRow(line = b01Line, avatarSize = B01_AVATAR_SIZE, highlighted = current.currentTurn == PlayerSide.B01)
            Spacer(Modifier.height(Spacing.xs))
            Row(
                Modifier.fillMaxWidth().padding(start = B01_AVATAR_SIZE + Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(current.b01HandSize) {
                    OrbitCardBackTile(width = 24.dp, height = 34.dp)
                    Spacer(Modifier.width(Spacing.xs))
                }
                Spacer(Modifier.weight(1f))
                if (current.b01Shielded) ShieldBadge()
                SignalDots(current.b01Signal)
            }

            Spacer(Modifier.height(Spacing.sm))

            // 가운데 테이블: B-01이 낸 카드 / 덱 더미(또는 라운드 종료 배너) / 내가 낸 카드.
            // 선택한 카드 미리보기는 여기 넣지 않는다(예전엔 여기 크게 펼쳐져 위아래 "낸 카드"가
            // 밀려 잘렸다) — 손패 위 선택 줄과 카드를 낼 때의 스포트라이트가 대신한다.
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .orbitTableSurface()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayedCardRow(label = "B-01이 낸 카드", summary = current.lastB01Card)
                    Spacer(Modifier.height(Spacing.sm))
                    HorizontalDivider(color = SpaceBlue)
                    Spacer(Modifier.height(Spacing.md))

                    val banner = roundEndBanner
                    if (banner != null) {
                        Text(roundEndBannerText(banner), color = GoldAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        // 라운드가 끝나면 서로 들고 있던 카드를 공개한다 — 진 이유를 납득할 수 있게.
                        Spacer(Modifier.height(Spacing.sm))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            RevealChip(owner = "내 패", card = banner.playerCard, winner = banner.winner == PlayerSide.PLAYER)
                            Text("vs", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            RevealChip(owner = "B-01 패", card = banner.b01Card, winner = banner.winner == PlayerSide.B01)
                        }
                        // 타이머로 자동으로 사라지지 않는다 — 직접 확인을 눌러야 다음 라운드로 넘어간다.
                        Spacer(Modifier.height(Spacing.sm))
                        GameButton(text = "확인", onClick = { viewModel.acknowledgeAndContinue() }, style = GameButtonStyle.Primary)
                    } else {
                        DeckPile(current.deckRemaining)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "DECK ${current.deckRemaining}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val firstPlayerLabel = if (current.roundFirstPlayer == PlayerSide.PLAYER) "나" else "B-01"
                        Text("이번 라운드 선공: $firstPlayerLabel", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(Modifier.height(Spacing.md))
                    HorizontalDivider(color = SpaceBlue)
                    Spacer(Modifier.height(Spacing.sm))
                    PlayedCardRow(label = "내가 낸 카드", summary = current.lastPlayerCard)
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // 선택 줄: 카드를 고르면 짧은 설명 + [사용], 아니면 안내 한 줄. 높이를 고정해 두어
            // 선택할 때마다 화면이 위아래로 튀지 않게 한다.
            // 카드를 낼 수 없는 이유(AI CORE 강제 사용 규칙, EMP인데 덱이 빔 등)도 여기 경고로 띄운다 —
            // 예전엔 화면 맨 위에 노란 글씨로 떠서 [사용]을 누른 쪽에선 잘 안 보였고 "왜 안 되지?"가 됐다.
            val selectedCard = selectedIndex?.let { current.playerHand.getOrNull(it) }
            SelectionBar(
                selectedCard = selectedCard,
                warning = message,
                hint = when {
                    roundEndBanner != null -> ""
                    canAct -> "사용할 카드를 선택하세요"
                    else -> "B-01 차례예요"
                },
                onPlay = { selectedCard?.let { confirmPlay(it) } }
            )

            Spacer(Modifier.height(Spacing.sm))

            // 아래쪽: 플레이어 바 + 내 앞에 놓인 손패(가운데 정렬). 고른 카드는 살짝 들려 올라온다.
            PlayerBar(
                signal = current.playerSignal,
                shielded = current.playerShielded,
                isTurn = current.currentTurn == PlayerSide.PLAYER
            )
            Spacer(Modifier.height(Spacing.md))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally)
            ) {
                current.playerHand.forEachIndexed { index, card ->
                    val selected = index == selectedIndex
                    val lift by animateDpAsState(if (selected) (-12).dp else 0.dp, label = "handLift")
                    Box(Modifier.offset(y = lift)) {
                        OrbitCardTile(
                            card = card,
                            enabled = canAct,
                            selected = selected,
                            size = handCardSize,
                            onClick = {
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                        )
                    }
                }
            }
        }

        // 카드가 놓이는 순간: 화면이 어두워지며 낸 카드가 가운데로 튀어나온다. 모달 창이 아니라
        // 게임 화면 위에 겹치는 연출 — 테이블 높이와 무관하게 화면 가운데라 작은 화면에서도 잘리지 않는다.
        var shownSpotlight by remember { mutableStateOf<PlayOrbitCardUseCase.PlayedCardSummary?>(null) }
        // 닫히는(페이드아웃) 동안에도 내용이 보이도록 마지막 값을 기억해 둔다.
        SideEffect { if (spotlight != null) shownSpotlight = spotlight }
        AnimatedVisibility(
            visible = spotlight != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.matchParentSize()
        ) {
            (spotlight ?: shownSpotlight)?.let { summary ->
                PlaySpotlight(summary = summary, onDismiss = { spotlight = null })
            }
        }
    }

    pendingScoutCard?.let { card ->
        ScoutGuessDialog(
            onGuess = { power ->
                viewModel.playCard(card, OrbitCardEffectInput.ScoutGuess(power))
                pendingScoutCard = null
                selectedIndex = null
            },
            onDismiss = { pendingScoutCard = null }
        )
    }
    if (showLeaveConfirm) {
        val betAmount = current.bet?.amount
        OrbitChoiceDialog(
            title = "게임 나가기",
            subtitle = if (betAmount != null) {
                "지금 나가면 이번 판은 패배로 처리되고\n베팅한 ${"%,d".format(betAmount)}코인을 잃어요."
            } else {
                "지금 나가면 이번 판은 패배로 처리돼요."
            },
            dismissText = "계속하기",
            onDismiss = { showLeaveConfirm = false }
        ) {
            GameButton(
                text = "나가기",
                onClick = {
                    showLeaveConfirm = false
                    viewModel.leaveMatch()
                    onExit()
                },
                modifier = Modifier.fillMaxWidth(),
                style = GameButtonStyle.Danger,
                size = GameButtonSize.Large
            )
        }
    }
    pendingEmpCard?.let { card ->
        EmpTargetDialog(
            onTarget = { target ->
                viewModel.playCard(card, OrbitCardEffectInput.EmpTarget(target))
                pendingEmpCard = null
                selectedIndex = null
            },
            onDismiss = { pendingEmpCard = null }
        )
    }
}

private val B01_AVATAR_SIZE = 52.dp
private val MAX_GAME_WIDTH = 520.dp
private val COMPACT_HEIGHT = 720.dp
private val SELECTION_BAR_MIN_HEIGHT = 64.dp
private const val SPOTLIGHT_DURATION_MS = 1800L

// 하단 플레이어 바: 시바 우주인 아바타(ch_planet_1의 헬멧 부분만 잘라서) + 쉴드 + SIGNAL.
@Composable
private fun PlayerBar(signal: Int, shielded: Boolean, isTurn: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpaceNavy)
                .border(1.dp, if (isTurn) GoldAccent else SpaceBlue, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(R.drawable.ch_planet_1),
                contentDescription = null,
                modifier = Modifier.requiredSize(64.dp).offset(y = 14.dp)
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text("나", color = if (isTurn) GoldAccent else TextPrimary, fontWeight = FontWeight.Bold)
        if (isTurn) {
            Spacer(Modifier.width(Spacing.sm))
            Text("내 차례", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.weight(1f))
        if (shielded) ShieldBadge()
        SignalDots(signal)
    }
}

// SIGNAL(먼저 SIGNALS_TO_WIN개 모으면 매치 승리)을 채워진 점/빈 점으로 표시.
@Composable
private fun SignalDots(signal: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        repeat(OrbitMatchState.SIGNALS_TO_WIN) { i ->
            val filled = i < signal
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (filled) GoldAccent else SpaceNavy)
                    .border(1.dp, if (filled) GoldAccent else TextSecondary, CircleShape)
            )
        }
    }
}

@Composable
private fun ShieldBadge() {
    Image(
        painter = painterResource(R.drawable.ic_orbit_shield),
        contentDescription = "SHIELD",
        modifier = Modifier.size(18.dp)
    )
    Spacer(Modifier.width(Spacing.sm))
}

// 덱 더미 — 뒷면 카드 몇 장을 살짝 어긋나게 겹쳐 쌓인 느낌을 낸다. 비면 빈 자리만 표시.
@Composable
private fun DeckPile(remaining: Int) {
    val layers = remaining.coerceIn(0, 3)
    Box(Modifier.size(width = 56.dp, height = 76.dp)) {
        if (layers == 0) {
            EmptyCardSlot(width = 48.dp, height = 66.dp)
        } else {
            repeat(layers) { i ->
                Box(Modifier.offset(x = (i * 3).dp, y = ((layers - 1 - i) * 3).dp)) {
                    OrbitCardBackTile(width = 48.dp, height = 66.dp)
                }
            }
        }
    }
}

// 진영별로 "마지막으로 낸 카드"를 테이블 위에 놓인 카드로 보여주는 자리. 다음 카드가 나오면
// 덮어써지고(히스토리 없음), 새 카드가 놓이는 순간 살짝 튀어오르듯 나타난다.
@Composable
private fun PlayedCardRow(label: String, summary: PlayOrbitCardUseCase.PlayedCardSummary?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (summary == null) {
            EmptyCardSlot(width = 56.dp, height = 76.dp)
        } else {
            val scale = remember(summary) { Animatable(0.6f) }
            LaunchedEffect(summary) {
                scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
            Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                OrbitCardTile(card = summary.card, enabled = true, size = OrbitCardSize.SMALL)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            if (summary == null) {
                Text("-", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            } else {
                Text(
                    "${cardLabel(summary.card.type)} (Power ${summary.card.power})",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (summary.blockedByShield) {
                    Text("→ 상대의 SHIELD에 막혔어요", color = StatusYellow, style = MaterialTheme.typography.labelSmall)
                }
                summary.noEffectNote?.let {
                    Text("→ $it", color = StatusYellow, style = MaterialTheme.typography.labelSmall)
                }
                summary.revealedOpponentCard?.let {
                    Text("→ 상대 카드: ${cardLabel(it.type)}", color = StatusYellow, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyCardSlot(width: Dp, height: Dp) {
    Box(
        Modifier
            .size(width = width, height = height)
            .border(1.dp, SpaceBlue, RoundedCornerShape(8.dp))
    )
}

private enum class OrbitCardSize(val width: Dp, val height: Dp, val iconSize: Dp, val showName: Boolean) {
    SMALL(56.dp, 76.dp, 30.dp, showName = false),
    NORMAL(96.dp, 128.dp, 44.dp, showName = true),
    LARGE(140.dp, 180.dp, 72.dp, showName = true),
    // 세로가 짧은 화면(폴드 펼침 등)의 손패용
    COMPACT(80.dp, 108.dp, 36.dp, showName = true)
}

// onClick이 없으면(테이블 위 카드·스포트라이트) 눌리지 않는 표시 전용 카드.
// 선택 표시는 흰 테두리 — 카드 고유색 중 CAPTAIN이 금색이라 금색 선택 표시와 겹치지 않게.
@Composable
private fun OrbitCardTile(
    card: OrbitCard,
    enabled: Boolean,
    selected: Boolean = false,
    size: OrbitCardSize = OrbitCardSize.NORMAL,
    onClick: (() -> Unit)? = null
) {
    val shape = pixelCardShape(small = size == OrbitCardSize.SMALL)
    Box(
        Modifier
            .width(size.width)
            .height(size.height)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
    ) {
        OrbitCardFace(card, size)
        if (selected) Box(Modifier.matchParentSize().border(2.dp, TextPrimary, shape))
    }
}

// 카드 앞면. 앱의 픽셀아트 톤에 맞춰 모든 마감을 "도트식"으로 한다 — 둥근 모서리 대신 잘린
// 모서리, 매끈한 그라데이션 대신 뚝 끊기는 3단 색 띠(계단식 음영), 어두운 외곽선 + 고유색 프레임에
// 위·왼쪽 밝은 선/아래·오른쪽 어두운 선(입체 테두리). 처음엔 둥근 모서리+그라데이션으로 그렸더니
// 디테일은 늘었지만 픽셀아트 느낌이 죽는다는 피드백.
@Composable
private fun OrbitCardFace(card: OrbitCard, size: OrbitCardSize) {
    val accent = cardAccent(card.type)
    val small = size == OrbitCardSize.SMALL
    val frame = when (size) {
        OrbitCardSize.SMALL -> 2.dp
        OrbitCardSize.LARGE -> 4.dp
        else -> 3.dp
    }
    Box(
        Modifier
            .fillMaxSize()
            .pixelFrame(
                accent = accent,
                bands = listOf(lerp(SpaceNavy, accent, 0.30f), lerp(SpaceNavy, accent, 0.12f), lerp(SpaceDark, SpaceNavy, 0.7f)),
                frame = frame,
                innerLine = !small,
                weights = CARD_BAND_WEIGHTS
            )
            .padding(frame + 1.dp)
    ) {
        Image(
            painter = painterResource(cardIconRes(card.type)),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).offset(y = if (size.showName) (-4).dp else 2.dp).size(size.iconSize)
        )
        // 숫자 배지 — 각진 사각형 + 어두운 외곽선
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(if (small) 1.dp else 3.dp)
                .background(SpaceDark)
                .padding(1.dp)
                .background(accent)
                .padding(horizontal = if (small) 3.dp else 5.dp)
        ) {
            Text(
                "${card.power}",
                color = SpaceDark,
                style = when (size) {
                    OrbitCardSize.SMALL -> MaterialTheme.typography.labelMedium
                    OrbitCardSize.COMPACT -> MaterialTheme.typography.labelLarge
                    OrbitCardSize.NORMAL -> MaterialTheme.typography.titleSmall
                    OrbitCardSize.LARGE -> MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold
            )
        }
        // 이름판 — 카드 아래 각진 띠
        if (size.showName) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(lerp(SpaceDark, accent, 0.35f))
                    .padding(vertical = if (size == OrbitCardSize.LARGE) 5.dp else 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    cardLabel(card.type),
                    color = TextPrimary,
                    style = if (size == OrbitCardSize.LARGE) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// 카드 모서리: 둥글게 깎지 않고 비스듬히 잘라 도트 계단 모서리처럼 보이게.
private fun pixelCardShape(small: Boolean) = pixelShape(if (small) 2.dp else 4.dp)

// 카드 바탕 3단 띠 비율(위 고유색이 비치는 띠 / 가운데 / 아래 어두운 띠).
private val CARD_BAND_WEIGHTS = listOf(0.3f, 0.4f, 0.3f)

// 덱 더미/상대 손패처럼 내용을 알 수 없는 카드를 나타내는 뒷면 타일. 앞면과 같은 도트식 프레임
// (파랑 계열)이라 한 벌의 카드처럼 보인다. 남색 카드·테이블 위에서도 윤곽이 묻히지 않게 한 톤 밝다.
@Composable
internal fun OrbitCardBackTile(width: Dp = 44.dp, height: Dp = 60.dp) {
    val tiny = width < 32.dp
    Box(
        Modifier
            .width(width)
            .height(height)
            .clip(pixelCardShape(small = tiny))
            .pixelFrame(
                accent = SpaceLight,
                bands = listOf(SpaceAccent, SpaceBlue, SpaceMid),
                frame = if (tiny) 1.dp else 2.dp,
                innerLine = !tiny,
                weights = CARD_BAND_WEIGHTS
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_orbit_card_back),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.72f)
        )
    }
}

// SCOUT DRONE·EMP처럼 카드를 낼 때 추가로 고르는 창의 공용 껍데기. 머티리얼 기본 AlertDialog가
// "샘플 앱" 같다는 피드백으로, 베팅 모달과 같은 패널(질감 남색 + 옅은 금색 테두리)에 카드 아이콘 제목.
@Composable
private fun OrbitChoiceDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    dismissText: String = "취소",
    content: @Composable ColumnScope.() -> Unit
) {
    // 앱 공용 확인 팝업(GameDialog) 틀 — 명판에 이름, 본문에 설명 + 고를 것, 아래 닫기 버튼.
    GameDialog(
        title = title,
        onDismissRequest = onDismiss,
        buttons = {
            GameButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = GameButtonStyle.Neutral,
                size = GameButtonSize.Large
            )
        }
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(subtitle, color = TextSecondary, style = BodyReading, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.lg))
            content()
        }
    }
}

// SCOUT DRONE: 추측할 카드를 실제 카드 모양(Power 2~8)으로 늘어놓고 탭해서 고른다.
// 숫자만으로는 "PROBE가 몇 번이었더라" 헷갈리기 쉬워 카드 이름도 아래 붙인다.
@Composable
private fun ScoutGuessDialog(onGuess: (Int) -> Unit, onDismiss: () -> Unit) {
    OrbitChoiceDialog(
        title = "SCOUT DRONE",
        subtitle = "B-01이 든 카드를 맞혀 보세요. 맞히면 B-01 OUT!",
        onDismiss = onDismiss
    ) {
        (2..8).chunked(4).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally)
            ) {
                row.forEach { power ->
                    val type = cardTypeForPower(power)
                    Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        OrbitCardTile(
                            card = OrbitCard(type),
                            enabled = true,
                            size = OrbitCardSize.SMALL,
                            onClick = { onGuess(power) }
                        )
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(
                            cardLabel(type),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

// EMP: 카드를 버리게 할 대상을 두 칸 중에서 고른다. 처음엔 캐릭터 얼굴(B-01·시바 우주인)을 넣었는데
// 두 그림의 질감이 서로 달라 어색하다는 피드백으로 글자만 둔다.
@Composable
private fun EmpTargetDialog(onTarget: (PlayerSide) -> Unit, onDismiss: () -> Unit) {
    OrbitChoiceDialog(
        title = "EMP",
        subtitle = "누구의 카드를 버리게 할까요? 고른 쪽은 새 카드를 받아요.",
        onDismiss = onDismiss
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            EmpTargetTile(label = "B-01", caption = "상대 카드를 버리게", modifier = Modifier.weight(1f)) {
                onTarget(PlayerSide.B01)
            }
            EmpTargetTile(label = "나", caption = "내 카드를 버리고 새로", modifier = Modifier.weight(1f)) {
                onTarget(PlayerSide.PLAYER)
            }
        }
    }
}

@Composable
private fun EmpTargetTile(label: String, caption: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(SpaceMid)
            .border(1.dp, SpaceBlue, shape)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.lg, horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = GoldAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.xxs))
        Text(caption, color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}


// 라운드 종료 때 공개되는 한쪽의 카드: "B-01 · [아이콘] CAPTAIN 8". 이긴 쪽은 금색 테두리.
@Composable
private fun RevealChip(owner: String, card: OrbitCard?, winner: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .clip(shape)
            .background(SpaceNavy)
            .border(1.dp, if (winner) GoldAccent else SpaceBlue, shape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$owner ", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        if (card == null) {
            Text("-", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        } else {
            Image(
                painter = painterResource(cardIconRes(card.type)),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(Spacing.xxs))
            Text(
                "${cardLabel(card.type)} ${card.power}",
                color = if (winner) GoldAccent else TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 손패 위 선택 줄. 경고가 있으면 경고(잠깐 떴다 사라짐), 고른 카드가 있으면 이름·짧은 설명 + [사용],
// 없으면 안내 한 줄.
@Composable
private fun SelectionBar(selectedCard: OrbitCard?, warning: String?, hint: String, onPlay: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = SELECTION_BAR_MIN_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        if (warning != null) {
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(lerp(SpaceNavy, StatusRed, 0.18f))
                    .border(1.5.dp, StatusRed, shape)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_ui_danger),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(warning, color = TextPrimary, style = BodyReading, fontWeight = FontWeight.Bold)
            }
        } else if (selectedCard == null) {
            Text(hint, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        } else {
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(SpaceNavy)
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), shape)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${cardLabel(selectedCard.type)} · Power ${selectedCard.power}",
                        color = GoldAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(cardShortDescription(selectedCard.type), color = TextSecondary, style = BodyReading)
                }
                Spacer(Modifier.width(Spacing.sm))
                GameButton(text = "사용", onClick = onPlay, style = GameButtonStyle.Primary)
            }
        }
    }
}

// 카드를 낸 순간의 연출: 어두운 막 + 가운데로 튀어나오는 큰 카드 + 결과 한 줄. 누르면 바로 닫힌다.
@Composable
private fun PlaySpotlight(summary: PlayOrbitCardUseCase.PlayedCardSummary, onDismiss: () -> Unit) {
    val scale = remember(summary) { Animatable(0.5f) }
    LaunchedEffect(summary) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(SpaceDark.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (summary.by == PlayerSide.PLAYER) "내가 낸 카드" else "B-01이 낸 카드",
                color = GoldAccent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.md))
            Box(Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value }) {
                OrbitCardTile(card = summary.card, enabled = true, selected = true, size = OrbitCardSize.LARGE)
            }
            Spacer(Modifier.height(Spacing.lg))
            val shape = RoundedCornerShape(10.dp)
            Text(
                spotlightResultText(summary),
                color = TextPrimary,
                style = BodyReading,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .background(SpaceNavy)
                    .border(1.dp, SpaceBlue, shape)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
        }
    }
}

// 스포트라이트 아래 결과 문구. 막힘/무효/OUT/확인 결과가 있으면 그걸, 없으면 카드가 한 일을 짧게.
private fun spotlightResultText(s: PlayOrbitCardUseCase.PlayedCardSummary): String {
    val byPlayer = s.by == PlayerSide.PLAYER
    val opponent = if (byPlayer) "B-01" else "나"
    return when {
        s.blockedByShield -> "상대의 SHIELD에 막혔어요"
        s.outSide != null -> {
            val who = if (s.outSide == PlayerSide.PLAYER) "내가" else "B-01이"
            if (s.discardedCard?.type == OrbitCardType.CAPTAIN) "CAPTAIN이 버려져 $who OUT됐어요!" else "$who OUT됐어요!"
        }
        s.noEffectNote != null -> s.noEffectNote
        s.revealedOpponentCard != null -> "B-01의 카드: ${cardLabel(s.revealedOpponentCard.type)}"
        else -> when (s.card.type) {
            OrbitCardType.SENSOR -> if (byPlayer) "B-01의 카드를 확인했어요" else "B-01이 내 카드를 확인했어요"
            OrbitCardType.SHIELD -> "다음 턴까지 ${opponent}의 카드 효과를 막아요"
            OrbitCardType.EMP -> "카드를 버리고 새로 받았어요"
            OrbitCardType.WARP_GATE -> "서로 카드를 맞바꿨어요"
            OrbitCardType.AI_CORE -> "아무 효과도 없어요"
            else -> cardShortDescription(s.card.type)
        }
    }
}

private fun cardTypeForPower(power: Int): OrbitCardType =
    OrbitCardType.entries.first { it.power == power }

private fun roundEndBannerText(info: OrbitRoundEndInfo): String {
    val outcome = when (info.winner) {
        PlayerSide.PLAYER -> "내가 승리!"
        PlayerSide.B01 -> "B-01 승리"
        null -> "무승부"
    }
    val cause = if (info.reason == RoundEndReason.OUT) "라운드 종료" else "덱 소진 — 라운드 종료"
    return "$cause — $outcome  (SIGNAL ${info.playerSignal}-${info.b01Signal})"
}
