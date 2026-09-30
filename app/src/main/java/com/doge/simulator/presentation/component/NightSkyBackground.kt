package com.doge.simulator.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import kotlin.random.Random

// 정거장 시설 하위 화면(휴게실·격납고·우주인 센터·연구소) 공통 배경 — 은은한 밤하늘.
// 단색 SpaceDark만 깔려 심심하던 화면에 잔잔한 분위기만 준다. 일러스트(정거장 내부 벽 패널)도
// 시도했지만 타일을 줄이고 어둡게 덮어도 "그림"으로 읽혀 과하다는 피드백으로, 에셋 없이
// 아래로 살짝 밝아지는 남색 그라데이션 + 드문드문한 정적 픽셀 별로 대체했다.
// 별은 움직이지 않는다 — 배경은 분위기용이라 애니메이션은 결정적 장면에만 쓴다.
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

private class Star(val x: Float, val y: Float, val sizeDp: Float, val alpha: Float)

// 픽셀 톤에 맞춰 원 대신 작은 사각형 점. 시드 고정이라 재구성·재진입해도 별 위치가 같다.
@Composable
private fun NightSkyStars(modifier: Modifier = Modifier, starCount: Int = 36) {
    val stars = remember(starCount) {
        val rnd = Random(20260930)
        List(starCount) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                sizeDp = if (rnd.nextFloat() < 0.12f) 2f else 1f,
                alpha = 0.15f + rnd.nextFloat() * 0.4f
            )
        }
    }
    Canvas(modifier) {
        stars.forEach { s ->
            val px = s.sizeDp.dp.toPx()
            drawRect(
                color = TextPrimary.copy(alpha = s.alpha),
                topLeft = Offset(s.x * size.width, s.y * size.height),
                size = Size(px, px)
            )
        }
    }
}
