package com.doge.simulator.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doge.simulator.R
import com.doge.simulator.ui.theme.ButtonDepth
import com.doge.simulator.ui.theme.ButtonPadding
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.NumericSmall
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.SpaceLight
import com.doge.simulator.ui.theme.SpaceMid
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.StatusGreen
import com.doge.simulator.ui.theme.StatusRed
import com.doge.simulator.ui.theme.StatusYellow
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary
import com.doge.simulator.ui.theme.textured
import kotlinx.coroutines.delay

// ─── 픽셀 테두리 카드 ─────────────────────────────────────────────
@Composable
fun PixelBorderCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.textured(shape = RoundedCornerShape(4.dp), baseColor = SpaceNavy),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Column(content = content) }
    )
}

// ─── 픽셀 버튼 ────────────────────────────────────────────────────
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SpaceBlue,
    contentColor: Color = GoldAccent,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonPadding.ctaInRow,
    minWidth: Dp = ButtonPadding.minWidth
) {
    Button(
        onClick = onClick,
        modifier = modifier.widthIn(min = minWidth),
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = SpaceMid,
            disabledContentColor = TextSecondary
        ),
        contentPadding = contentPadding
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── 픽셀 스탯 칩 ─────────────────────────────────────────────────
@Composable
fun PixelStatChip(
    label: String,
    value: String,
    valueColor: Color = GoldAccent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = SpaceBlue.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Text(text = value, color = valueColor, style = NumericSmall)
        }
    }
}

// ─── 행성 레벨 뱃지 ────────────────────────────────────────────────
@Composable
fun PlanetLevelBadge(level: Int, modifier: Modifier = Modifier) {
    val color = when {
        level >= 15 -> StatusRed
        level >= 10 -> StatusYellow
        else -> StatusGreen
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = "Lv.$level",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ─── 시세 변동 플래시 텍스트 ─────────────────────────────────────
/**
 * value가 변경될 때 이전보다 크면 초록, 작으면 빨강으로 잠깐 번쩍인 뒤
 * [defaultColor]로 돌아온다
 */
@Composable
fun FlashingValueText(
    value: Long,
    text: String,
    defaultColor: Color = GoldAccent,
    modifier: Modifier = Modifier
) {
    var targetColor by remember { mutableStateOf(defaultColor) }
    var prevValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value > prevValue) targetColor = StatusGreen
        else if (value < prevValue) targetColor = StatusRed
        prevValue = value
        if (targetColor != defaultColor) {
            delay(1100)
            targetColor = defaultColor
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(350),
        label = "flashColor"
    )

    Text(text = text, color = animatedColor, style = NumericSmall, modifier = modifier)
}

// ─── 탐험 버튼 펄스 스케일 ─────────────────────────────────────────
/**
 * 아이들 상태(탐험 중 아닐 때)에 버튼을 살짝 박동시켜 클릭 유도
 */
@Composable
fun pulsingScale(enabled: Boolean): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "buttonPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 1.035f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    return scale
}

// ─── 라이브 코인 티커 ─────────────────────────────────────────────
/**
 * [baseCoins] DB 잔액 기준으로 [netPerMin]/분 만큼 매 초 시각적으로 증가.
 * DB 업데이트 시 자동 리셋.
 */
@Composable
fun rememberLiveCoinDisplay(baseCoins: Long, netPerMin: Long): Long {
    var tickCount by remember(baseCoins) { mutableStateOf(0L) }
    LaunchedEffect(baseCoins) {
        tickCount = 0L
        if (netPerMin > 0L) {
            while (true) {
                delay(1000L)
                tickCount++
            }
        }
    }
    return baseCoins + (tickCount * netPerMin / 60L)
}

// ─── 화면별 컨텍스트 바텀 바 ──────────────────────────────────────
/**
 * 홈이 아닌 화면 하단에 고정되는 액션 바
 * - 왼쪽: 홈으로 버튼 (항상)
 * - 오른쪽: 화면별 보조 버튼 (optional)
 */
@Composable
fun ContextualBottomBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SpaceNavy.copy(alpha = 0.97f),
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        border = BorderStroke(1.dp, SpaceBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            horizontalArrangement = if (secondaryLabel != null) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onHomeClick,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpaceLight,
                    contentColor = TextPrimary
                ),
                border = ButtonDepth.highlightBorder,
                elevation = ButtonDepth.elevation(),
                contentPadding = ButtonPadding.ctaInRow
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_home),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("홈으로", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (secondaryLabel != null && onSecondaryClick != null) {
                Button(
                    onClick = onSecondaryClick,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = SpaceDark
                    ),
                    border = ButtonDepth.highlightBorder,
                    elevation = ButtonDepth.elevation(),
                    contentPadding = ButtonPadding.ctaInRow
                ) {
                    Text(secondaryLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ─── 글로우 알파 (카드 테두리 등에 활용) ─────────────────────────
@Composable
fun glowAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    return alpha
}