package com.doge.simulator.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.TextPrimary
import kotlin.math.sin
import kotlin.random.Random

// 하위 화면(휴게실·격납고·우주인 센터·연구소·행성 상세·탐사 일지) 공통 배경 — 은은한 밤하늘.
// 단색 SpaceDark만 깔려 심심하던 화면에 잔잔한 분위기만 준다. 일러스트(정거장 내부 벽 패널)도
// 시도했지만 타일을 줄이고 어둡게 덮어도 "그림"으로 읽혀 과하다는 피드백으로, 에셋 없이
// 아래로 살짝 밝아지는 남색 그라데이션 + 드문드문한 픽셀 별로 대체했다.
// 별은 제자리에서 천천히 반짝이기만 한다(유저 요청, 2026-09-30) — 위치는 움직이지 않는다.
@Composable
fun NightSkyBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.background(Brush.verticalGradient(listOf(SpaceDark, NIGHT_SKY_BOTTOM)))) {
        NightSkyStars(Modifier.fillMaxSize())
        content()
    }
}

private val NIGHT_SKY_BOTTOM = lerp(SpaceDark, SpaceNavy, 0.6f)

private class Star(
    val x: Float,
    val y: Float,
    val sizeDp: Float,
    val phase: Float,
    val speed: Float
)

// 픽셀 톤에 맞춰 원 대신 작은 사각형 점. 시드 고정이라 재구성·재진입해도 별 위치가 같다.
// 각 별이 자기 위상·속도로 밝기가 오르내리고(탐험 화면 TwinklingStars와 같은 방식), 큰 별(2dp)은
// 밝을 때 상하좌우로 1칸짜리 십자 반짝임이 붙는다.
@Composable
private fun NightSkyStars(modifier: Modifier = Modifier, starCount: Int = 60) {
    val stars = remember(starCount) {
        val rnd = Random(20260930)
        List(starCount) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                sizeDp = if (rnd.nextFloat() < 0.15f) 2f else 1f,
                phase = rnd.nextFloat() * TWO_PI,
                speed = rnd.nextFloat() * 0.8f + 0.6f
            )
        }
    }
    val time by rememberInfiniteTransition(label = "nightSky").animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing)),
        label = "nightSkyTime"
    )
    Canvas(modifier) {
        stars.forEach { s ->
            val twinkle = (sin(time * s.speed + s.phase) + 1f) / 2f
            val alpha = 0.15f + 0.65f * twinkle
            val px = s.sizeDp.dp.toPx()
            val topLeft = Offset(s.x * size.width, s.y * size.height)
            drawRect(color = TextPrimary.copy(alpha = alpha), topLeft = topLeft, size = Size(px, px))
            if (s.sizeDp > 1f && twinkle > 0.6f) {
                val arm = 1.dp.toPx()
                val armColor = TextPrimary.copy(alpha = alpha * 0.6f)
                drawRect(armColor, Offset(topLeft.x, topLeft.y - arm), Size(px, arm))
                drawRect(armColor, Offset(topLeft.x, topLeft.y + px), Size(px, arm))
                drawRect(armColor, Offset(topLeft.x - arm, topLeft.y), Size(arm, px))
                drawRect(armColor, Offset(topLeft.x + px, topLeft.y), Size(arm, px))
            }
        }
    }
}

private const val TWO_PI = (2 * Math.PI).toFloat()
