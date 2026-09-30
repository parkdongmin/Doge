package com.doge.simulator.presentation.screen.orbit

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

// ORBIT 게임 테이블 장식. 에셋 없이 Compose로만 그린다.
// (화면 뒤 별 배경은 휴게실·하위 화면과 통일해 공용 NightSkyBackground를 쓴다.)

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
