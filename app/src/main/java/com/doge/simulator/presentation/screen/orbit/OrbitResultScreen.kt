package com.doge.simulator.presentation.screen.orbit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.domain.model.orbit.OrbitBetSettlement
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

@Composable
fun OrbitResultScreen(
    onPlayAgain: () -> Unit,
    onReturnToLounge: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val settlement by viewModel.lastSettlement.collectAsState()

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
            Text(
                if (won) "YOU WIN!" else "YOU LOSE...",
                color = if (won) StatusGreen else StatusRed,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.lg))
            val label = if (won) "WIN +${current.netChange}" else "RESULT ${current.netChange}"
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
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
