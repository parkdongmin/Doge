package com.doge.simulator.presentation.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// 상단 바·ⓘ 등 UI 크롬용 픽셀 아이콘. 머티리얼 기본 아이콘(ArrowBack/Info)이 픽셀 톤 앱에서
// 혼자 "샘플 앱"처럼 보여서 교체했다. 이미지 에셋 대신 픽셀 격자를 ImageVector로 만들어
// Icon(tint=...)을 그대로 쓸 수 있고 어떤 크기에서도 선명하다.
object PixelIcons {

    val Back: ImageVector = pixelIcon(
        "Back",
        "............",
        ".....##.....",
        "....##......",
        "...##.......",
        "..##........",
        ".##########.",
        ".##########.",
        "..##........",
        "...##.......",
        "....##......",
        ".....##.....",
        "............"
    )

    val Info: ImageVector = pixelIcon(
        "Info",
        "...######...",
        ".##......##.",
        ".#...##...#.",
        "#....##....#",
        "#..........#",
        "#...###....#",
        "#....##....#",
        "#....##....#",
        "#....##....#",
        ".#..####..#.",
        ".##......##.",
        "...######..."
    )
}

// '#' 칸을 채운 픽셀 아이콘. 한 줄에서 연속된 칸은 사각형 하나로 합쳐 칸 사이 이음새를 줄인다.
private fun pixelIcon(name: String, vararg rows: String): ImageVector {
    val width = rows.maxOf { it.length }.toFloat()
    val height = rows.size.toFloat()
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = width,
        viewportHeight = height
    ).path(fill = SolidColor(Color.Black)) {
        rows.forEachIndexed { y, row ->
            var x = 0
            while (x < row.length) {
                if (row[x] != '#') { x++; continue }
                val start = x
                while (x < row.length && row[x] == '#') x++
                moveTo(start.toFloat(), y.toFloat())
                lineTo(x.toFloat(), y.toFloat())
                lineTo(x.toFloat(), y + 1f)
                lineTo(start.toFloat(), y + 1f)
                close()
            }
        }
    }.build()
}
