package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

// 베팅 화면 — 금액(4단계 고정) + 위험도 티어(3단계, 최저 티어는 항상 선택 가능) 선택 후 시작.
@Composable
fun OrbitBetScreen(
    onBack: () -> Unit,
    onMatchStarted: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val coins by viewModel.coins.collectAsState()
    val selectedAmount by viewModel.selectedBetAmount.collectAsState()
    val selectedTier by viewModel.selectedRiskTier.collectAsState()
    val uiSnapshot by viewModel.uiSnapshot.collectAsState()

    LaunchedEffect(uiSnapshot) {
        if (uiSnapshot != null) onMatchStarted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
            .padding(Spacing.xl)
    ) {
        Text("ORBIT 베팅", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("보유 재화 ${coins}원", color = TextSecondary, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(Spacing.xl))
        Text("베팅 금액", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.sm))
        FlowRowAmounts(
            amounts = viewModel.betAmounts,
            coins = coins,
            selected = selectedAmount,
            onSelect = viewModel::selectBetAmount
        )

        Spacer(Modifier.height(Spacing.xl))
        Text("위험도", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.sm))
        viewModel.riskTiers.forEach { tier ->
            RiskTierRow(tier, selected = selectedTier == tier, onSelect = { viewModel.selectRiskTier(tier) })
            Spacer(Modifier.height(Spacing.sm))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = viewModel::startMatch,
            enabled = selectedAmount != null && (selectedAmount ?: 0L) <= coins,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GAME START")
        }
        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("휴게실로", color = TextSecondary)
        }
    }
}

@Composable
private fun FlowRowAmounts(
    amounts: List<Long>,
    coins: Long,
    selected: Long?,
    onSelect: (Long) -> Unit
) {
    Column {
        amounts.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { amount ->
                    val affordable = amount <= coins
                    OutlinedButton(
                        onClick = { onSelect(amount) },
                        enabled = affordable,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected == amount) SpaceAccent else SpaceNavy
                        )
                    ) {
                        Text("${amount}원", color = if (affordable) TextPrimary else TextDisabled)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun RiskTierRow(tier: OrbitRiskTier, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (selected) SpaceAccent else SpaceMid),
        shape = RoundedCornerShape(12.dp),
        onClick = onSelect
    ) {
        Row(
            Modifier.padding(Spacing.md).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(tier.displayName, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("승리 배율 x${tier.winRewardMultiplier}", color = GoldAccent)
        }
    }
}
