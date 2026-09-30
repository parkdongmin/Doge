package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doge.simulator.R
import com.doge.simulator.domain.model.orbit.OrbitRiskTier
import com.doge.simulator.presentation.component.PixelButton
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

// 베팅 모달 — 휴게실에서 ORBIT "입장"을 누르면 뜬다. 금액(4단계 고정) + 위험도 티어(3단계, 최저
// 티어는 항상 선택 가능)를 고르고 바로 게임 시작. 예전엔 별도 베팅 화면이었는데, 고를 게 두 가지뿐이라
// 화면 하나를 통째로 쓰니 휑하고 "샘플 앱" 같다는 피드백으로 모달로 옮겼다.
// 매치가 시작되면(uiSnapshot != null) 휴게실 쪽에서 게임 화면으로 이동시킨다.
@Composable
fun OrbitBetDialog(viewModel: OrbitViewModel, onDismiss: () -> Unit) {
    val coins by viewModel.coins.collectAsState()
    val selectedAmount by viewModel.selectedBetAmount.collectAsState()
    val selectedTier by viewModel.selectedRiskTier.collectAsState()
    val line = remember { B01Lines.bet.random() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(16.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .textured(shape = shape, baseColor = SpaceNavy),
            shape = shape,
            color = Color.Transparent,
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(Spacing.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ORBIT",
                        color = GoldAccent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_ui_coin),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("%,d".format(coins), color = GoldAccent, style = NumericXSmall)
                }

                Spacer(Modifier.height(Spacing.md))
                B01SpeechRow(line = line, avatarSize = 40.dp)

                Spacer(Modifier.height(Spacing.lg))
                SectionLabel("베팅 금액")
                Spacer(Modifier.height(Spacing.sm))
                viewModel.betAmounts.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        row.forEach { amount ->
                            SelectTile(
                                selected = selectedAmount == amount,
                                enabled = amount <= coins,
                                onClick = { viewModel.selectBetAmount(amount) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_ui_coin),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(Spacing.xs))
                                    Text("%,d".format(amount), color = TextPrimary, style = NumericXSmall)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }

                Spacer(Modifier.height(Spacing.sm))
                SectionLabel("위험도")
                Spacer(Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    viewModel.riskTiers.forEach { tier ->
                        RiskTierTile(
                            tier = tier,
                            selected = selectedTier == tier,
                            onClick = { viewModel.selectRiskTier(tier) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text("위험도가 높을수록 B-01이 실수를 덜 해요.", color = TextSecondary, style = BodyReading)

                Spacer(Modifier.height(Spacing.xl))
                PixelButton(
                    text = "게임 시작",
                    onClick = viewModel::startMatch,
                    enabled = selectedAmount != null && (selectedAmount ?: 0L) <= coins,
                    containerColor = GoldAccent,
                    contentColor = SpaceDark,
                    contentPadding = ButtonPadding.fullWidthCta,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.xs))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("닫기", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

// 금액·위험도 선택 칸. 선택되면 금색 테두리 + 한 톤 밝은 바탕.
@Composable
private fun SelectTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        shape = shape,
        color = if (selected) SpaceBlue else SpaceMid.copy(alpha = 0.6f),
        border = if (selected) BorderStroke(1.5.dp, GoldAccent) else BorderStroke(1.dp, SpaceBlue)
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = Spacing.md), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun RiskTierTile(tier: OrbitRiskTier, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectTile(selected = selected, onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tier.displayName, color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("x${tier.winRewardMultiplier}", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
        }
    }
}
