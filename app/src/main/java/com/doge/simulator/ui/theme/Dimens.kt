package com.doge.simulator.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── 여백 스케일 (4dp 그리드) ─────────────────────────────────────
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

// ─── 버튼 padding / 최소 폭 토큰 ───────────────────────────────────
object ButtonPadding {
    /** 인라인 텍스트 버튼 — "광고 보기" 류 링크 */
    val textInline = PaddingValues(horizontal = 8.dp, vertical = 4.dp)

    /** 리스트/카드 행 안의 구매·고용·강화·매도 액션 버튼 */
    val listItemAction = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    /** 행 안에 단독으로 놓이는 CTA (전체 폭 아님) */
    val ctaInRow = PaddingValues(horizontal = 20.dp, vertical = 10.dp)

    /** 전체 폭 기본 CTA */
    val fullWidthCta = PaddingValues(vertical = 14.dp)

    /** 텍스트가 바뀌어도(연구/MAX, 구매/코인 부족 등) 버튼 폭이 흔들리지 않게 하는 최소 폭 */
    val minWidth: Dp = 72.dp
}

// ─── 버튼 입체감 (그림자 + 상단 하이라이트) ─────────────────────────
/**
 * 색은 단색으로 유지하되, 은은한 그림자와 상단 하이라이트 선으로 살짝 떠 있는 느낌을 준다.
 * sample.png의 버튼들처럼 면 자체는 플랫하고 가장자리만 입체적인 스타일.
 */
object ButtonDepth {
    /** 상단이 밝고 아래로 갈수록 투명해지는 테두리 — 위에서 빛을 받는 느낌 */
    val highlightBorder = BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)))

    @Composable
    fun elevation(): ButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 4.dp,
        pressedElevation = 1.dp,
        disabledElevation = 0.dp
    )
}
