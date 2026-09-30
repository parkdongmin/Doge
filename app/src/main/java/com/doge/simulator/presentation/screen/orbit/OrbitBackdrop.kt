package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.doge.simulator.presentation.component.pixelFrame
import com.doge.simulator.presentation.component.pixelShape
import com.doge.simulator.ui.theme.*
import kotlin.random.Random

// ORBIT 화면 공용 배경 장식. 에셋 없이 Compose로만 그린다.
// 배경은 분위기용이라 움직이지 않는다(정적) — 애니메이션은 카드가 놓이는 순간 같은 결정적
// 장면에만 쓴다.

private class Star(val x: Float, val y: Float, val sizeDp: Float, val alpha: Float)

// 화면 뒤에 까는 정적인 별 배경. 픽셀 톤에 맞춰 원 대신 작은 사각형 점으로 그린다.
// 시드를 고정해 재구성·재진입해도 별 위치가 바뀌지 않는다.
@Composable
fun OrbitStarfield(modifier: Modifier = Modifier, starCount: Int = 70) {
    val stars = remember(starCount) {
        val rnd = Random(20260930)
        List(starCount) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                sizeDp = if (rnd.nextFloat() < 0.15f) 2f else 1f,
                alpha = 0.2f + rnd.nextFloat() * 0.5f
            )
        }
    }
    Canvas(modifier.fillMaxSize().background(SpaceDark)) {
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

// 카드 테이블 표면: 카드와 같은 도트식 마감(잘린 모서리 + 3단 색 띠 + 입체 테두리) 위에 가운데
// 궤도 링 두 겹. 링은 스크롤되는 내용 뒤(이 modifier가 붙은 바깥 노드)에 그려져 고정돼 있다.
fun Modifier.orbitTableSurface(): Modifier =
    this
        .clip(pixelShape(6.dp))
        .pixelFrame(
            accent = SpaceBlue,
            bands = listOf(SpaceMid, lerp(SpaceMid, SpaceNavy, 0.5f), SpaceNavy),
            frame = 2.dp,
            innerLine = true
        )
        .drawBehind {
            val ringColor = SpaceLight.copy(alpha = 0.14f)
            val stroke = Stroke(width = 1.dp.toPx())
            listOf(0.9f to 0.5f, 0.62f to 0.32f).forEach { (w, h) ->
                val ringSize = Size(size.width * w, size.height * h)
                drawOval(
                    color = ringColor,
                    topLeft = Offset((size.width - ringSize.width) / 2, (size.height - ringSize.height) / 2),
                    size = ringSize,
                    style = stroke
                )
            }
        }
