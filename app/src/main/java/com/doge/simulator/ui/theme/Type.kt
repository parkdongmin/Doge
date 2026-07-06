package com.doge.simulator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.doge.simulator.R

// ─── 픽셀 아트 타이틀 / 레이블용 ─────────────────────────────────
val PfStardustFamily = FontFamily(
    Font(R.font.pf_stardust_extrabold, FontWeight.ExtraBold)
)

// ─── 숫자 / 데이터 표시용 (가독성 최우선) ────────────────────────
val PretendardFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal)
)

// ─── 숫자 전용 TextStyle (Pretendard) ────────────────────────────
/** 히어로 수치 — 코인 잔액 등 가장 크고 중요한 숫자 */
val NumericXLarge = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 38.sp,
    lineHeight = 44.sp,
    letterSpacing = (-1).sp
)

/** 카드 주요 수치 — 총 자산, 행성 시세 */
val NumericLarge = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.5).sp
)

/** 중간 수치 — 생산량, 유지비 등 */
val NumericMedium = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 19.sp,
    lineHeight = 25.sp,
    letterSpacing = 0.sp
)

/** 보조 수치 — StatRow 값, 뱃지 안 숫자 */
val NumericSmall = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp
)

/** 타임스탬프, 보조 레이블 숫자 */
val NumericXSmall = TextStyle(
    fontFamily = PretendardFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

// ─── 앱 전체 Typography (PfStardust) ─────────────────────────────
val PixelTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 1.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PfStardustFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )
)