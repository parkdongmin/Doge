package com.doge.simulator.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceDark

// 도트식(픽셀아트) 패널 마감 — ORBIT 카드·게임 테이블 공용(베팅 모달은 시도해 봤지만 원래 모양 유지로 결정).
// 둥근 모서리·매끈한 그라데이션으로 그리면 디테일은 늘어도 앱의 픽셀아트 느낌이 죽는다는 피드백으로,
// 잘린 모서리 + 뚝 끊기는 색 띠(계단식 음영) + 입체 테두리로 마감한다.

// 모서리를 둥글게 깎지 않고 비스듬히 잘라 도트 계단 모서리처럼 보이게 한다. clip()에 같이 쓴다.
fun pixelShape(cut: Dp) = CutCornerShape(cut)

// 바깥부터: 어두운 외곽선 1칸 → accent 프레임(위·왼쪽 한 줄은 밝게, 아래·오른쪽 한 줄은 어둡게 —
// 입체감) → 안쪽 바탕은 위에서부터 bands 색 띠(경계가 딱 떨어짐, weights 비율) → (선택) 바탕 가장자리
// 안쪽에 옅은 accent 선 한 줄. 모서리는 호출 쪽에서 clip(pixelShape(..))으로 자른다.
fun Modifier.pixelFrame(
    accent: Color,
    bands: List<Color>,
    frame: Dp,
    innerLine: Boolean = false,
    weights: List<Float> = List(bands.size) { 1f / bands.size }
): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val o = 1.dp.toPx()
    val f = frame.toPx()
    drawRect(SpaceDark)
    drawRect(accent, Offset(o, o), Size(w - 2 * o, h - 2 * o))
    val light = lerp(accent, Color.White, 0.45f)
    val dark = lerp(accent, SpaceDark, 0.5f)
    drawRect(light, Offset(o, o), Size(w - 2 * o, o))
    drawRect(light, Offset(o, o), Size(o, h - 2 * o))
    drawRect(dark, Offset(o, h - 2 * o), Size(w - 2 * o, o))
    drawRect(dark, Offset(w - 2 * o, o), Size(o, h - 2 * o))

    val inset = o + f
    val innerW = w - 2 * inset
    val innerH = h - 2 * inset
    var y = inset
    bands.forEachIndexed { i, color ->
        val bandH = if (i == bands.lastIndex) inset + innerH - y else innerH * weights[i]
        drawRect(color, Offset(inset, y), Size(innerW, bandH))
        y += bandH
    }
    if (innerLine) {
        val gap = 2.dp.toPx()
        drawRect(
            accent.copy(alpha = 0.35f),
            Offset(inset + gap, inset + gap),
            Size(innerW - 2 * gap, innerH - 2 * gap),
            style = Stroke(width = o)
        )
    }
}

