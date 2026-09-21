package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity

// 휴게실 — 정거장 하위 진입점. B-01/카드 테이블(=ORBIT 진입)과 일일 리워드 광고 버튼을 둔다.
@Composable
fun LoungeScreen(
    onBack: () -> Unit,
    onEnterOrbit: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val coins by viewModel.coins.collectAsState()
    val dailyAdRemaining by viewModel.dailyAdRemaining.collectAsState()
    val message by viewModel.message.collectAsState()
    val activity = LocalContext.current.findActivity()
    var showRules by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshDailyAdRemaining() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
            .padding(Spacing.xl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("휴게실", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("보유 재화 ${coins}원", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onBack) {
                Text("정거장으로", color = GoldAccent)
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── B-01 & 카드 테이블 ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEnterOrbit),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(Spacing.xl)) {
                Text("B-01", color = GoldAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "\"다음 목적지까지 08:42 남았습니다. 한 판 하시겠습니까?\"",
                    color = TextSecondary,
                    style = BodyReading
                )
                Spacer(Modifier.height(Spacing.lg))
                Button(onClick = onEnterOrbit, modifier = Modifier.fillMaxWidth()) {
                    Text("카드 테이블 — ORBIT")
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // ── 일일 리워드 광고 ───────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SpaceMid),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(Spacing.lg)) {
                Text("일일 지원금", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Spacing.xxs))
                Text("광고 시청 후 재화를 받아요. 오늘 남은 횟수: $dailyAdRemaining", color = TextSecondary, style = BodyReading)
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick = { viewModel.claimDailyAdReward(activity) },
                    enabled = dailyAdRemaining > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (dailyAdRemaining > 0) "광고 보고 재화 받기" else "오늘 지원금 소진")
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        TextButton(onClick = { showRules = true }) {
            Text("ⓘ ORBIT 규칙 보기", color = GoldAccent)
        }

        message?.let {
            Spacer(Modifier.height(Spacing.md))
            Text(it, color = StatusGreen, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showRules) {
        OrbitRulesDialog(onDismiss = { showRules = false })
    }
}
