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

// ─── 라벨보다 한 단계 작은 보조 텍스트 (PfStardust) ───────────────
/** Typography.labelSmall(10sp) 아래 단계 — 뱃지/미니스탯 등 더 작은 보조 라벨 */
val LabelTiny = TextStyle(
    fontFamily = PfStardustFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 9.sp,
    lineHeight = 11.sp,
    letterSpacing = 0.5.sp
)

// ─── 아이콘 크기 (Image 아이콘 dp 변환 + 일부 글리프 Text용) ──────────
/**
 * 커스텀 이미지 아이콘의 크기(.value.dp로 변환해 사용)와,
 * "›" 같은 순수 타이포그래피 글리프의 fontSize로 쓰는 스케일.
 * 실사용 클러스터(14~16 / 18~20 / 26~28 / 36~48sp)를 4단계로 정리했다.
 */
object IconGlyphSize {
    val small = 16.sp   // 인라인 텍스트 옆 아이콘
    val medium = 20.sp  // 리스트 행/카드 아이콘
    val large = 28.sp   // 카드 헤더·잠금 아이콘
    val xlarge = 48.sp  // 빈 상태/스플래시 아이콘
}

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