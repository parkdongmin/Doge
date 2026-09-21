package com.doge.simulator.presentation.screen.orbit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import com.doge.simulator.domain.usecase.orbit.PlayOrbitCardUseCase
import com.doge.simulator.presentation.viewmodel.OrbitRoundEndInfo
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

// ORBIT 게임 화면.
//
// 카드 사용 결과는 "마지막으로 낸 카드" 슬롯(진영당 하나)에 계속 남아있다가 그 진영이 다음
// 카드를 내는 순간 덮어써진다 — 타이머나 확인 버튼 없이도 방금 무슨 일이 있었는지 내 페이스로
// 읽을 수 있고, 그 이전 기록까지는 남지 않아 전체 히스토리를 만들지는 않는다(FR-004).
// 라운드가 끝나는 것만은 별도로 명시적인 "확인"을 받는다 — SIGNAL만 조용히 바뀌고 지나가면
// 라운드가 끝난 건지 헷갈린다는 피드백 반영.
//
// 카드 선택은 2단계다: 손패에서 카드를 누르면 먼저 "선택"만 되어 위쪽에 크게 뜨고 전체 설명이
// 나온다(바로 발동되지 않음) — 다시 한번 "이 카드 사용"을 눌러야 실제로 낸다. 실수로 CAPTAIN을
// 눌러 즉시 OUT되는 사고 등을 막기 위함.
@Composable
fun OrbitGameScreen(
    onExit: () -> Unit,
    onMatchFinished: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val snapshot by viewModel.uiSnapshot.collectAsState()
    val roundEndBanner by viewModel.roundEndBanner.collectAsState()
    var selectedCard by remember { mutableStateOf<OrbitCard?>(null) }
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
        if (roundEndBanner != null || snapshot?.currentTurn != PlayerSide.PLAYER) selectedCard = null
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
                selectedCard = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
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

        Spacer(Modifier.height(Spacing.md))

        // 상단 게임 영역: B-01 + B-01이 마지막으로 낸 카드
        SidePanel(
            name = "B-01",
            handCount = current.b01HandSize,
            signal = current.b01Signal,
            shielded = current.b01Shielded,
            isTurn = current.currentTurn == PlayerSide.B01
        )
        Spacer(Modifier.height(Spacing.xs))
        LastPlaySlot(label = "B-01의 마지막 카드", summary = current.lastB01Card)

        Spacer(Modifier.height(Spacing.md))

        // 중앙: 덱 잔여 수 / 라운드 종료 배너 / 선택한 카드 미리보기
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpaceMid),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                Modifier.padding(Spacing.md).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("남은 카드 ${current.deckRemaining}장", color = TextSecondary, style = MaterialTheme.typography.bodySmall)

                roundEndBanner?.let { info ->
                    Spacer(Modifier.height(Spacing.sm))
                    Text(roundEndBannerText(info), color = GoldAccent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    // 타이머로 자동으로 사라지지 않는다 — 직접 확인을 눌러야 다음 라운드로 넘어간다.
                    Spacer(Modifier.height(Spacing.sm))
                    Button(onClick = { viewModel.acknowledgeAndContinue() }) {
                        Text("확인")
                    }
                }

                // 손패에서 카드를 선택하면(아직 내지는 않은 상태) 여기 크게 미리보기 + 전체
                // 설명이 뜬다. "이 카드 사용"을 눌러야 실제로 발동한다.
                if (roundEndBanner == null) {
                    selectedCard?.let { card ->
                        Spacer(Modifier.height(Spacing.md))
                        HorizontalDivider(color = SpaceBlue)
                        Spacer(Modifier.height(Spacing.md))
                        OrbitCardTile(card = card, enabled = true, selected = true, large = true, onClick = {})
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            cardFullDescription(card.type),
                            color = TextSecondary,
                            style = BodyReading,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            OutlinedButton(onClick = { selectedCard = null }) { Text("선택 취소") }
                            Button(onClick = { confirmPlay(card) }) { Text("이 카드 사용") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 하단: 내가 마지막으로 낸 카드 + 손패
        LastPlaySlot(label = "내 마지막 카드", summary = current.lastPlayerCard)
        Spacer(Modifier.height(Spacing.xs))
        SidePanel(
            name = "나",
            handCount = current.playerHand.size,
            signal = current.playerSignal,
            shielded = current.playerShielded,
            isTurn = current.currentTurn == PlayerSide.PLAYER
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            current.playerHand.forEach { card ->
                OrbitCardTile(
                    card = card,
                    enabled = canAct,
                    selected = card == selectedCard,
                    onClick = {
                        selectedCard = if (selectedCard == card) null else card
                    }
                )
            }
        }
    }

    pendingScoutCard?.let { card ->
        ScoutGuessDialog(
            onGuess = { power ->
                viewModel.playCard(card, OrbitCardEffectInput.ScoutGuess(power))
                pendingScoutCard = null
                selectedCard = null
            },
            onDismiss = { pendingScoutCard = null }
        )
    }
    pendingEmpCard?.let { card ->
        EmpTargetDialog(
            onTarget = { target ->
                viewModel.playCard(card, OrbitCardEffectInput.EmpTarget(target))
                pendingEmpCard = null
                selectedCard = null
            },
            onDismiss = { pendingEmpCard = null }
        )
    }
}

@Composable
private fun SidePanel(name: String, handCount: Int, signal: Int, shielded: Boolean, isTurn: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = if (isTurn) GoldAccent else TextPrimary, fontWeight = FontWeight.Bold)
            if (shielded) {
                Spacer(Modifier.width(Spacing.xs))
                Text("🛡", color = StatusGreen)
            }
            Spacer(Modifier.width(Spacing.sm))
            Text("카드 ${handCount}장", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Row {
            repeat(3) { i ->
                Text(if (i < signal) "●" else "○", color = GoldAccent)
            }
        }
    }
}

// 진영별로 "마지막으로 낸 카드"만 계속 보여주는 자리. 다음 카드가 나오면 덮어써진다.
@Composable
private fun LastPlaySlot(label: String, summary: PlayOrbitCardUseCase.PlayedCardSummary?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        if (summary == null) {
            Text("-", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        } else {
            Column {
                Text(
                    "${cardLabel(summary.card.type)} (Power ${summary.card.power})",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                if (summary.blockedByShield) {
                    Text("→ 상대의 SHIELD에 막혔어요", color = StatusYellow, style = MaterialTheme.typography.labelSmall)
                }
                summary.revealedOpponentCard?.let {
                    Text("→ 상대 카드: ${cardLabel(it.type)}", color = StatusYellow, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun OrbitCardTile(
    card: OrbitCard,
    enabled: Boolean,
    selected: Boolean = false,
    large: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (large) 140.dp to 180.dp else 96.dp to 128.dp
    Card(
        modifier = Modifier.width(size.first).height(size.second),
        colors = CardDefaults.cardColors(containerColor = if (enabled) SpaceNavy else SpaceMid),
        shape = RoundedCornerShape(10.dp),
        border = if (selected) BorderStroke(2.dp, GoldAccent) else null,
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${card.power}",
                color = GoldAccent,
                style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                cardLabel(card.type),
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            Text(
                cardEffectShort(card.type),
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
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

private fun cardEffectShort(type: OrbitCardType): String = when (type) {
    OrbitCardType.SCOUT_DRONE -> "Power 추측"
    OrbitCardType.SENSOR -> "카드 열람"
    OrbitCardType.PROBE -> "Power 비교"
    OrbitCardType.SHIELD -> "효과 무효화"
    OrbitCardType.EMP -> "카드 교체"
    OrbitCardType.WARP_GATE -> "카드 교환"
    OrbitCardType.AI_CORE -> "효과 없음"
    OrbitCardType.CAPTAIN -> "사용 시 OUT"
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
