package com.doge.simulator.presentation.screen.orbit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitBetSettlement
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

// 결과 화면 — 승/패 마스코트는 전용 일러스트(ch_result_win/ch_result_lose)를 쓴다.
@Composable
fun OrbitResultScreen(
    onPlayAgain: () -> Unit,
    onReturnToLounge: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val settlement by viewModel.lastSettlement.collectAsState()
    val coins by viewModel.coins.collectAsState()

    // "다시 하기"/"휴게실로"를 누르면 뷰모델이 lastSettlement를 즉시 null로 지우는데, 이
    // 화면이 사라지기 전에 그 null이 먼저 반영되면 won이 false로 떨어져 승리했는데도 화면을
    // 나가는 순간 잠깐 "YOU LOSE"가 스쳐 보이는 버그가 있었다. 한 번 받은 정산 결과는
    // null로 되돌아가도 화면에서는 계속 마지막 값을 보여준다("sticky").
    var displayedSettlement by remember { mutableStateOf<OrbitBetSettlement?>(null) }
    LaunchedEffect(settlement) {
        if (settlement != null) displayedSettlement = settlement
    }

    // 시스템/제스처 뒤로가기도 "휴게실로"와 동일하게 처리 — 그냥 화면만 닫히면 끝난 매치의
    // 스냅샷/정산 결과가 뷰모델에 남아있게 되어, 다음에 베팅 화면에 들어갔을 때 그 잔여
    // 스냅샷을 보고 곧장 게임 화면으로 튀어버리는 버그로 이어졌다.
    BackHandler {
        viewModel.returnToLounge()
        onReturnToLounge()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val current = displayedSettlement
        if (current == null) {
            // 매치 종료 직후 정산이 아직 끝나지 않은 아주 짧은 순간(비동기 처리 중)
            CircularProgressIndicator(color = GoldAccent)
        } else {
            val won = current.outcome == MatchOutcome.WON
            Image(
                painter = painterResource(if (won) R.drawable.ch_result_win else R.drawable.ch_result_lose),
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                if (won) "YOU WIN!" else "YOU LOSE...",
                color = if (won) StatusGreen else StatusRed,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.lg))
            val line = remember(won) { (if (won) B01Lines.resultWon else B01Lines.resultLost).random() }
            B01SpeechRow(line = line, avatarSize = 40.dp)
            Spacer(Modifier.height(Spacing.md))
            SettlementBreakdown(settlement = current, coins = coins)
        }
        Spacer(Modifier.height(Spacing.xxl))
        Button(
            onClick = { viewModel.playAgain(); onPlayAgain() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("다시 하기") }
        Spacer(Modifier.height(Spacing.sm))
        OutlinedButton(
            onClick = { viewModel.returnToLounge(); onReturnToLounge() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("휴게실로") }
    }
}

// BET(건 돈) / WIN·RESULT(이번 판 순손익) / TOTAL(정산 후 잔여 재화) 세 줄 내역.
// netChange는 승리 시 배율이 적용된 "지급액 전체"(베팅액 회수분 포함)라, WIN 줄에는
// 거기서 베팅액을 뺀 순수익만 보여준다 — 그래야 BET+WIN을 더한 값이 실제 지급액과 맞는다.
@Composable
private fun SettlementBreakdown(settlement: OrbitBetSettlement, coins: Long) {
    val won = settlement.outcome == MatchOutcome.WON
    val profit = settlement.netChange - settlement.betAmount
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SpaceMid),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(Spacing.lg).fillMaxWidth()) {
            BreakdownRow("BET", "%,d원".format(settlement.betAmount), TextPrimary)
            Spacer(Modifier.height(Spacing.xs))
            if (won) {
                BreakdownRow("WIN", "+%,d원".format(profit), StatusGreen)
            } else {
                BreakdownRow("RESULT", "%,d원".format(settlement.netChange), StatusRed)
            }
            Spacer(Modifier.height(Spacing.xs))
            HorizontalDivider(color = SpaceBlue)
            Spacer(Modifier.height(Spacing.xs))
            BreakdownRow("TOTAL", "%,d원".format(coins), GoldAccent)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
