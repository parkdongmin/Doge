package com.doge.simulator.presentation.screen.orbit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.orbit.OrbitCard
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.domain.model.orbit.PlayerSide
import com.doge.simulator.domain.model.orbit.RoundEndReason
import com.doge.simulator.domain.usecase.orbit.OrbitCardEffectInput
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

// ORBIT 게임 화면. 카드가 사용되는 순간에만 일시적으로 결과를 보여주고(FR-004), 지금까지
// 사용된 카드의 지속 열람 목록은 두지 않는다 — 플레이어가 직접 기억해야 하는 요소.
@Composable
fun OrbitGameScreen(
    onExit: () -> Unit,
    onMatchFinished: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val snapshot by viewModel.uiSnapshot.collectAsState()
    val lastPlayed by viewModel.lastPlayedCard.collectAsState()
    val roundEndBanner by viewModel.roundEndBanner.collectAsState()
    var pendingScoutCard by remember { mutableStateOf<OrbitCard?>(null) }
    var pendingEmpCard by remember { mutableStateOf<OrbitCard?>(null) }

    // 매치가 방금 끝났더라도, 마지막 수의 결과(lastPlayed)나 라운드 종료 배너를 플레이어가
    // 아직 "확인"하지 않았다면 곧바로 결과 화면으로 넘기지 않는다 — 안 그러면 승부를 가른
    // 장면을 볼 새도 없이 화면이 바뀌어버린다.
    LaunchedEffect(snapshot?.matchOver, lastPlayed, roundEndBanner) {
        if (snapshot?.matchOver == true && lastPlayed == null && roundEndBanner == null) {
            onMatchFinished()
        }
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

        // 상단 게임 영역: B-01
        SidePanel(
            name = "B-01",
            handCount = current.b01HandSize,
            signal = current.b01Signal,
            shielded = current.b01Shielded,
            isTurn = current.currentTurn == PlayerSide.B01
        )

        Spacer(Modifier.height(Spacing.md))

        // 중앙: 덱/일시 공개 연출
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
                lastPlayed?.let { summary ->
                    Spacer(Modifier.height(Spacing.xs))
                    val who = if (summary.by == PlayerSide.PLAYER) "나" else "B-01"
                    Text(
                        "$who → ${cardLabel(summary.card.type)} 사용!",
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    summary.revealedOpponentCard?.let {
                        Text("상대 카드: ${cardLabel(it.type)}", color = StatusYellow)
                    }
                    // 타이머로 자동으로 사라지지 않는다 — 다 읽었으면 직접 확인을 눌러야 다음
                    // 턴으로 넘어간다.
                    Spacer(Modifier.height(Spacing.sm))
                    Button(onClick = { viewModel.acknowledgePlayedCard() }) {
                        Text("확인")
                    }
                }
                roundEndBanner?.let { info ->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(roundEndBannerText(info), color = GoldAccent, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(Spacing.sm))
                    Button(onClick = { viewModel.acknowledgeRoundEnd() }) {
                        Text("확인")
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 하단: 플레이어 손패
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
                    enabled = current.currentTurn == PlayerSide.PLAYER &&
                        lastPlayed == null && roundEndBanner == null,
                    onClick = {
                        when (card.type) {
                            OrbitCardType.SCOUT_DRONE -> pendingScoutCard = card
                            OrbitCardType.EMP -> pendingEmpCard = card
                            else -> viewModel.playCard(card)
                        }
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
            },
            onDismiss = { pendingScoutCard = null }
        )
    }
    pendingEmpCard?.let { card ->
        EmpTargetDialog(
            onTarget = { target ->
                viewModel.playCard(card, OrbitCardEffectInput.EmpTarget(target))
                pendingEmpCard = null
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

@Composable
private fun OrbitCardTile(card: OrbitCard, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(96.dp).height(128.dp),
        colors = CardDefaults.cardColors(containerColor = if (enabled) SpaceNavy else SpaceMid),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            Modifier.fillMaxSize().padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${card.power}", color = GoldAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                cardLabel(card.type),
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                cardEffectShort(card.type),
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    TextButton(onClick = { onGuess(power) }) { Text("Power $power") }
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

private fun roundEndBannerText(info: com.doge.simulator.presentation.viewmodel.OrbitRoundEndInfo): String =
    when (info.reason) {
        RoundEndReason.DRAW -> "덱 소진 — 무승부! 새 라운드를 시작해요."
        RoundEndReason.DECK_EXHAUSTED -> {
            val who = if (info.winner == PlayerSide.PLAYER) "내가" else "B-01이"
            "덱 소진 — $who 라운드를 가져갔어요!"
        }
        RoundEndReason.OUT -> "" // 이 배너는 OUT일 때는 쓰지 않는다
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
