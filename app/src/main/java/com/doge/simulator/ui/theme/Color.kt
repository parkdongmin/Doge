package com.doge.simulator.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 우주 배경 계열 (#24448F 기준, 더 어두운 방향)
val SpaceDark = Color(0xFF00020E)
val SpaceNavy = Color(0xFF0D1B3E)
val SpaceMid = Color(0xFF162B5B)
val SpaceBlue = Color(0xFF24448F)
val SpaceAccent = Color(0xFF3A6FD8)
val SpaceLight = Color(0xFF5B8FFF)

// 스플래시·로그인 배경 그라데이션 — 대부분 어둡게 두고 하단 1/3에서만 우주 블루가 올라옴.
// (인게임 화면들은 거의 검정이라, 브랜드 화면 두 개만 이걸로 살짝 띄운다)
val BrandBackgroundGradient: Brush = Brush.verticalGradient(
    0f to Color(0xFF0A1225),
    0.62f to Color(0xFF0A1225),
    1f to Color(0xFF24448F)
)

// 강조 색상
val GoldAccent = Color(0xFFFEDC56)
val GoldDim = Color(0xFFB89C2A)

// 상태 색상
val StatusGreen = Color(0xFF2ECC71)
val StatusGreenDim = Color(0xFF1A7A44)
val StatusYellow = Color(0xFFF1C40F)
val StatusYellowDim = Color(0xFF8A6E00)
val StatusRed = Color(0xFFE74C3C)
val StatusRedDim = Color(0xFF8B1A0F)

// 텍스트
val TextPrimary = Color(0xFFE8EBF5)
val TextSecondary = Color(0xFF7B8BB5)
val TextDisabled = Color(0xFF3D4A6B)

// 레거시 (기존 코드 호환)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
