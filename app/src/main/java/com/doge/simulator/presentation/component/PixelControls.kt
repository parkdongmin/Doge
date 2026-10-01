package com.doge.simulator.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doge.simulator.ui.theme.*

// 머티리얼 기본 부품(진행 바·로딩 스피너·스위치·숫자 입력칸)이 픽셀 게임 화면에서 혼자 "일반 앱" 같아서
// 도트식으로 다시 만든 것들(2026-10-02).

// 칸 단위로 차는 진행 바 — 매끈하게 차오르는 막대 대신 블록이 하나씩 켜진다.
@Composable
fun PixelProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = SpaceAccent,
    segments: Int = 20,
    height: Dp = 8.dp
) {
    val filled = (progress.coerceIn(0f, 1f) * segments).let { kotlin.math.ceil(it).toInt() }
        .let { if (progress <= 0f) 0 else it }
    Row(
        modifier
            .fillMaxWidth()
            .height(height + 4.dp)
            .background(SpaceDark)
            .border(1.dp, SpaceMid)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(segments) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (i < filled) color else SpaceMid.copy(alpha = 0.35f))
            )
        }
    }
}

// 로딩 — 빙글 도는 원 대신 픽셀 점 세 개가 차례로 밝아진다.
@Composable
fun PixelLoading(modifier: Modifier = Modifier, color: Color = GoldAccent, dotSize: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "pixel_loading")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "pixel_loading_phase"
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(dotSize * 0.75f), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val active = phase.toInt() == i
            Box(
                Modifier
                    .size(dotSize)
                    .background(color.copy(alpha = if (active) 1f else 0.3f))
            )
        }
    }
}

// 켜기/끄기 토글 — 각진 받침 위에 네모 손잡이, 켜지면 파랑 받침 + 오른쪽 손잡이 + "ON".
@Composable
fun PixelToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .size(width = 56.dp, height = 28.dp)
            .clip(shape)
            .background(if (checked) SpaceAccent else SpaceMid)
            .border(1.dp, if (checked) SpaceLight else SpaceBlue, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(3.dp)
    ) {
        Text(
            if (checked) "ON" else "OFF",
            color = if (checked) Color.White else TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(if (checked) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 4.dp)
        )
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(22.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (checked) Color.White else TextSecondary)
        )
    }
}

// 수량 고르기 — [−] 숫자 [+] 와 아래 "1개 / 절반 / 최대". 숫자 칸은 눌러서 직접 입력도 된다.
// (머티리얼 입력칸에 키보드로만 넣던 방식 대신, 게임의 수량 선택처럼.)
@Composable
fun QuantityStepper(
    value: Long,
    max: Long,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            GameButton(
                text = "−",
                onClick = { onValueChange((value - 1).coerceAtLeast(1)) },
                enabled = value > 1,
                style = GameButtonStyle.Neutral,
                size = GameButtonSize.Large,
                modifier = Modifier.width(56.dp)
            )
            val shape = RoundedCornerShape(8.dp)
            BasicTextField(
                value = if (value > 0) "%,d".format(value) else "",
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(12)
                    onValueChange((digits.toLongOrNull() ?: 0L).coerceAtMost(max))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = NumericSmall.copy(color = GoldAccent, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(GoldAccent),
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(SpaceDark)
                    .border(1.dp, SpaceBlue, shape)
                    .padding(vertical = Spacing.md, horizontal = Spacing.sm)
            )
            GameButton(
                text = "+",
                onClick = { onValueChange((value + 1).coerceAtMost(max)) },
                enabled = value < max,
                style = GameButtonStyle.Neutral,
                size = GameButtonSize.Large,
                modifier = Modifier.width(56.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            GameButton(text = "1개", onClick = { onValueChange(1L.coerceAtMost(max)) }, style = GameButtonStyle.Neutral, modifier = Modifier.weight(1f))
            GameButton(text = "절반", onClick = { onValueChange((max / 2).coerceAtLeast(1L).coerceAtMost(max)) }, style = GameButtonStyle.Neutral, modifier = Modifier.weight(1f))
            GameButton(text = "최대", onClick = { onValueChange(max) }, style = GameButtonStyle.Neutral, modifier = Modifier.weight(1f))
        }
    }
}
