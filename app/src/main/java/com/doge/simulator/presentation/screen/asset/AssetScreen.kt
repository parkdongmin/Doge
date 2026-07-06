package com.doge.simulator.presentation.screen.asset

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.presentation.viewmodel.AssetViewModel
import com.doge.simulator.ui.theme.*

@Composable
fun AssetScreen(
    onRankClick: () -> Unit = {},
    viewModel: AssetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val displayCoins by viewModel.liveCoins.collectAsState()
    val displayTotalAsset by viewModel.liveTotalAsset.collectAsState()
    val resources = state.resources

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
            .navigationBarsPadding()
    ) {
        // ── 헤더 ──────────────────────────────────────────────────────
        Text(
            text = "나의 자산",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "보유 자산과 자원 현황을 확인하세요",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 총 자산 히어로 카드 ────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "총 자산 (DGT)",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "%,.2f".format(displayTotalAsset.toDouble()),
                            color = GoldAccent,
                            style = NumericXLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (state.netProductionPerMin > 0L) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "⚡", fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+%,d".format(state.netProductionPerMin),
                                    color = StatusGreen,
                                    style = NumericSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "/ 분",
                                    color = StatusGreen.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 보유 자원 섹션 ─────────────────────────────────────────────
        SectionHeader(title = "보유 자원")
        Spacer(modifier = Modifier.height(10.dp))

        if (resources.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                color = SpaceNavy.copy(alpha = 0.85f), border = BorderStroke(1.dp, SpaceMid)) {
                Text("탐사를 통해 자원을 획득하세요", color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = androidx.compose.ui.Modifier.padding(16.dp))
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, SpaceMid)) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    resources.forEachIndexed { index, resource ->
                        ResourceRow(
                            iconRes = resource.type.iconRes,
                            name = resource.type.displayName,
                            amount = "%,d".format(resource.amount),
                            changePercent = null,
                            changePositive = true
                        )
                        if (index < resources.size - 1) {
                            HorizontalDivider(color = SpaceMid.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 보유 자산 섹션 ─────────────────────────────────────────────
        SectionHeader(title = "보유 자산")
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, SpaceMid)
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                ResourceRow(
                    icon = "🟡",
                    name = "DGT",
                    amount = "%,.2f".format(displayCoins.toDouble()),
                    changePercent = if (state.netProductionPerMin > 0L)
                        "+%,d / 분".format(state.netProductionPerMin) else null,
                    changePositive = true
                )
                HorizontalDivider(color = SpaceMid.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                ResourceRow(
                    icon = "🟢",
                    name = "USDT",
                    amount = "%,.2f".format(state.totalMarketValue.toDouble() / 1000.0),
                    changePercent = null,
                    changePositive = true
                )
                HorizontalDivider(color = SpaceMid.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                ResourceRow(
                    icon = "🌌",
                    name = "NFT 자산",
                    amount = "${state.planetCount}",
                    changePercent = null,
                    changePositive = true,
                    unit = "개"
                )

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 요약 스탯 ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = "📊",
                label = "행성 시세 합계",
                value = "%,d".format(state.totalMarketValue),
                unit = "코인"
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = "📈",
                label = "누적 순수익",
                value = "%,d".format(state.totalProfit),
                unit = "코인",
                valueColor = StatusGreen
            )
        }

        if (state.netProductionPerMin > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = StatusGreen.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚡", fontSize = 16.sp)
                    Text(
                        text = "분당 순생산",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "+%,d / 분".format(state.netProductionPerMin),
                        color = StatusGreen,
                        style = NumericSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRankClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceAccent),
            border = BorderStroke(1.dp, SpaceAccent.copy(alpha = 0.5f))
        ) {
            Text(text = "🏆  랭킹 보기", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ResourceRow(
    name: String,
    amount: String,
    changePercent: String?,
    changePositive: Boolean,
    unit: String = "",
    @DrawableRes iconRes: Int? = null,
    icon: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(iconRes),
                contentDescription = name,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(text = icon.orEmpty(), fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amount $unit",
                color = GoldAccent,
                style = NumericSmall,
                fontWeight = FontWeight.SemiBold
            )
            changePercent?.let {
                Text(
                    text = it,
                    color = if (changePositive) StatusGreen else StatusRed,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    unit: String,
    valueColor: Color = TextPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, SpaceMid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = valueColor, style = NumericSmall, fontWeight = FontWeight.Bold)
            Text(text = unit, color = valueColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

