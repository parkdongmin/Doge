package com.doge.simulator.presentation.screen.orbit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.OrbitMatchState
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase
import com.doge.simulator.presentation.viewmodel.OrbitRoundEndInfo
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

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
// 카드 선택은 2단계다: 손패에서 카드를 누르면 먼저 "선택"만 되어 테이블 가운데 크게 뜨고 전체
// 설명이 나온다(바로 발동되지 않음) — 다시 한번 "이 카드 사용"을 눌러야 실제로 낸다. 실수로
// CAPTAIN을 눌러 즉시 OUT되는 사고 등을 막기 위함.
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
    BackHandler {
        viewModel.leaveMatch()
        onExit()
    }

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

    Box(Modifier.fillMaxSize()) {
        OrbitStarfield()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                    TextButton(onClick = { viewModel.leaveMatch(); onExit() }) {
                        Text("나가기", color = StatusRed)
                    }
                }
            }

            // 카드를 낼 수 없는 이유(예: AI CORE 강제 사용 규칙, EMP인데 덱이 빔) 안내.
            message?.let {
                Spacer(Modifier.height(Spacing.sm))
                Text(it, color = StatusYellow, style = MaterialTheme.typography.bodySmall)
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

            // 가운데 테이블: B-01이 낸 카드 / 덱 더미(또는 라운드 종료 배너·선택 카드 미리보기) / 내가
            // 낸 카드. 작은 기기에서 미리보기까지 펼쳐지면 넘칠 수 있어 테이블 안쪽만 스크롤된다.
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
                    val previewCard = selectedIndex?.let { current.playerHand.getOrNull(it) }
                    when {
                        banner != null -> {
                            Text(roundEndBannerText(banner), color = GoldAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            // 타이머로 자동으로 사라지지 않는다 — 직접 확인을 눌러야 다음 라운드로 넘어간다.
                            Spacer(Modifier.height(Spacing.sm))
                            Button(onClick = { viewModel.acknowledgeAndContinue() }) {
                                Text("확인")
                            }
                        }
                        // 손패에서 카드를 선택하면(아직 내지는 않은 상태) 여기 크게 미리보기 + 전체
                        // 설명이 뜬다. "이 카드 사용"을 눌러야 실제로 발동한다.
                        previewCard != null -> {
                            OrbitCardTile(card = previewCard, enabled = true, selected = true, size = OrbitCardSize.LARGE)
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                cardFullDescription(previewCard.type),
                                color = TextSecondary,
                                style = BodyReading,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(Spacing.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                OutlinedButton(onClick = { selectedIndex = null }) { Text("선택 취소") }
                                Button(onClick = { confirmPlay(previewCard) }) { Text("이 카드 사용") }
                            }
                        }
                        else -> {
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
                    }

                    Spacer(Modifier.height(Spacing.md))
                    HorizontalDivider(color = SpaceBlue)
                    Spacer(Modifier.height(Spacing.sm))
                    PlayedCardRow(label = "내가 낸 카드", summary = current.lastPlayerCard)
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // 아래쪽: 플레이어 바 + 손패
            PlayerBar(
                signal = current.playerSignal,
                shielded = current.playerShielded,
                isTurn = current.currentTurn == PlayerSide.PLAYER
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                current.playerHand.forEachIndexed { index, card ->
                    OrbitCardTile(
                        card = card,
                        enabled = canAct,
                        selected = index == selectedIndex,
                        onClick = {
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    )
                }
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
    LARGE(140.dp, 180.dp, 72.dp, showName = true)
}

// onClick이 없으면(테이블 위 카드·미리보기) 눌리지 않는 표시 전용 카드.
@Composable
private fun OrbitCardTile(
    card: OrbitCard,
    enabled: Boolean,
    selected: Boolean = false,
    size: OrbitCardSize = OrbitCardSize.NORMAL,
    onClick: (() -> Unit)? = null
) {
    val modifier = Modifier.width(size.width).height(size.height)
    val colors = CardDefaults.cardColors(containerColor = if (enabled) SpaceNavy else SpaceMid)
    val shape = RoundedCornerShape(if (size == OrbitCardSize.SMALL) 8.dp else 10.dp)
    val border = if (selected) BorderStroke(2.dp, GoldAccent) else null
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, enabled = enabled, colors = colors, shape = shape, border = border) {
            OrbitCardFace(card, enabled, size)
        }
    } else {
        Card(modifier = modifier, colors = colors, shape = shape, border = border) {
            OrbitCardFace(card, enabled, size)
        }
    }
}

@Composable
private fun OrbitCardFace(card: OrbitCard, enabled: Boolean, size: OrbitCardSize) {
    Column(
        Modifier.fillMaxSize().padding(if (size == OrbitCardSize.SMALL) Spacing.xs else Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${card.power}",
            color = GoldAccent,
            style = when (size) {
                OrbitCardSize.SMALL -> MaterialTheme.typography.labelLarge
                OrbitCardSize.NORMAL -> MaterialTheme.typography.titleMedium
                OrbitCardSize.LARGE -> MaterialTheme.typography.headlineSmall
            },
            fontWeight = FontWeight.Bold
        )
        Image(
            painter = painterResource(cardIconRes(card.type)),
            contentDescription = null,
            modifier = Modifier.size(size.iconSize),
            alpha = if (enabled) 1f else 0.5f
        )
        if (size.showName) {
            Text(
                cardLabel(card.type),
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        } else {
            Spacer(Modifier.height(Spacing.xxs))
        }
    }
}

// 덱 더미/상대 손패처럼 내용을 알 수 없는 카드를 나타내는 뒷면 타일.
// 바탕이 남색 카드·테이블 위에 놓이는 경우가 많아, 같은 남색으로 칠하면 카드 윤곽이 묻혀
// 엠블럼만 붕 떠 보였다 — 한 톤 밝은 바탕 + 밝은 테두리로 "카드 한 장" 형태가 보이게 한다.
@Composable
internal fun OrbitCardBackTile(width: Dp = 44.dp, height: Dp = 60.dp) {
    Card(
        modifier = Modifier.width(width).height(height),
        colors = CardDefaults.cardColors(containerColor = SpaceBlue),
        shape = RoundedCornerShape(if (width < 32.dp) 4.dp else 6.dp),
        border = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.55f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_orbit_card_back),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f)
            )
        }
    }
}

@Composable
private fun ScoutGuessDialog(onGuess: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("SCOUT DRONE — Power 추측") },
        text = {
            Column {
                (2..8).forEach { power ->
                    // Power 숫자만으로는 "PROBE가 몇 번이었더라" 하고 헷갈리기 쉬워, 카드
                    // 이름도 함께 보여준다.
                    TextButton(onClick = { onGuess(power) }) {
                        Text("$power · ${cardLabel(cardTypeForPower(power))}")
                    }
                }
            }
        }
    )
}

@Composable
private fun EmpTargetDialog(onTarget: (PlayerSide) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("EMP — 대상 선택") },
        text = {
            Column {
                TextButton(onClick = { onTarget(PlayerSide.PLAYER) }) { Text("나") }
                TextButton(onClick = { onTarget(PlayerSide.B01) }) { Text("B-01") }
            }
        }
    )
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

private fun cardLabel(type: OrbitCardType): String = when (type) {
    OrbitCardType.SCOUT_DRONE -> "SCOUT DRONE"
    OrbitCardType.SENSOR -> "SENSOR"
    OrbitCardType.PROBE -> "PROBE"
    OrbitCardType.SHIELD -> "SHIELD"
    OrbitCardType.EMP -> "EMP"
    OrbitCardType.WARP_GATE -> "WARP GATE"
    OrbitCardType.AI_CORE -> "AI CORE"
    OrbitCardType.CAPTAIN -> "CAPTAIN"
}

private fun cardIconRes(type: OrbitCardType): Int = when (type) {
    OrbitCardType.SCOUT_DRONE -> R.drawable.ic_orbit_scout_drone
    OrbitCardType.SENSOR -> R.drawable.ic_orbit_sensor
    OrbitCardType.PROBE -> R.drawable.ic_orbit_probe
    OrbitCardType.SHIELD -> R.drawable.ic_orbit_shield
    OrbitCardType.EMP -> R.drawable.ic_orbit_emp
    OrbitCardType.WARP_GATE -> R.drawable.ic_orbit_warp_gate
    OrbitCardType.AI_CORE -> R.drawable.ic_orbit_ai_core
    OrbitCardType.CAPTAIN -> R.drawable.ic_orbit_captain
}

private fun cardFullDescription(type: OrbitCardType): String = when (type) {
    OrbitCardType.SCOUT_DRONE ->
        "상대가 가진 카드의 Power를 하나 추측해요. 맞히면 상대는 즉시 OUT! Power 1(SCOUT DRONE 자신)은 지목할 수 없어요."
    OrbitCardType.SENSOR ->
        "상대가 지금 들고 있는 카드를 확인해요."
    OrbitCardType.PROBE ->
        "서로의 카드 Power를 비교해서 낮은 쪽이 즉시 OUT돼요. 같으면 아무 일도 없어요."
    OrbitCardType.SHIELD ->
        "다음 내 턴이 시작될 때까지 상대의 카드 효과를 전부 막아줘요."
    OrbitCardType.EMP ->
        "나 또는 상대 중 한 명을 골라, 그 사람이 지금 든 카드를 버리고 새 카드를 받게 해요. 덱에 카드가 없으면 쓸 수 없어요."
    OrbitCardType.WARP_GATE ->
        "나와 상대가 가진 카드를 서로 맞바꿔요."
    OrbitCardType.AI_CORE ->
        "따로 효과는 없어요. 하지만 EMP나 WARP GATE와 함께 손에 있으면 반드시 이 카드를 내야 해요."
    OrbitCardType.CAPTAIN ->
        "ORBIT에서 가장 강한 카드예요. 하지만 직접 내거나 EMP로 버려지면 즉시 OUT돼요."
}
