package com.doge.simulator.presentation.screen.orbit

import androidx.compose.ui.graphics.Color
import com.doge.simulator.R
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.ui.theme.*

// 카드 종류별 표시 정보(이름·아이콘·고유색·한 줄 설명) — 게임 화면과 규칙 창(OrbitRulesDialog)이 같이 쓴다.

// 카드별 고유색. 금색은 앱에서 "귀한 것" 전용이라 가장 강한 CAPTAIN에만 쓴다.
internal fun cardAccent(type: OrbitCardType): Color = when (type) {
    OrbitCardType.SCOUT_DRONE -> Color(0xFF4FC3F7)
    OrbitCardType.SENSOR -> SpaceLight
    OrbitCardType.PROBE -> Color(0xFF9C8CFF)
    OrbitCardType.SHIELD -> StatusGreen
    OrbitCardType.EMP -> StatusRed
    OrbitCardType.WARP_GATE -> Color(0xFFD17CE8)
    OrbitCardType.AI_CORE -> Color(0xFF26D0CE)
    OrbitCardType.CAPTAIN -> GoldAccent
}

internal fun cardLabel(type: OrbitCardType): String = when (type) {
    OrbitCardType.SCOUT_DRONE -> "SCOUT DRONE"
    OrbitCardType.SENSOR -> "SENSOR"
    OrbitCardType.PROBE -> "PROBE"
    OrbitCardType.SHIELD -> "SHIELD"
    OrbitCardType.EMP -> "EMP"
    OrbitCardType.WARP_GATE -> "WARP GATE"
    OrbitCardType.AI_CORE -> "AI CORE"
    OrbitCardType.CAPTAIN -> "CAPTAIN"
}

internal fun cardIconRes(type: OrbitCardType): Int = when (type) {
    OrbitCardType.SCOUT_DRONE -> R.drawable.ic_orbit_scout_drone
    OrbitCardType.SENSOR -> R.drawable.ic_orbit_sensor
    OrbitCardType.PROBE -> R.drawable.ic_orbit_probe
    OrbitCardType.SHIELD -> R.drawable.ic_orbit_shield
    OrbitCardType.EMP -> R.drawable.ic_orbit_emp
    OrbitCardType.WARP_GATE -> R.drawable.ic_orbit_warp_gate
    OrbitCardType.AI_CORE -> R.drawable.ic_orbit_ai_core
    OrbitCardType.CAPTAIN -> R.drawable.ic_orbit_captain
}

// 카드 기본 효과만 한 줄로. 선택 줄처럼 좁은 곳에 뜨므로 예외 규칙(SCOUT DRONE은 Power 1 지목 불가,
// AI CORE 강제 사용 등)은 빼고 "이 카드가 뭘 하는지"만 — 예외 규칙은 어기려 할 때 경고로 안내한다.
internal fun cardShortDescription(type: OrbitCardType): String = when (type) {
    OrbitCardType.SCOUT_DRONE -> "상대 카드의 Power를 맞히면 상대가 OUT돼요."
    OrbitCardType.SENSOR -> "상대가 든 카드를 확인해요."
    OrbitCardType.PROBE -> "카드 Power를 비교해 낮은 쪽이 OUT돼요."
    OrbitCardType.SHIELD -> "다음 내 턴까지 상대 카드 효과를 막아요."
    OrbitCardType.EMP -> "한 명의 카드를 버리고 새로 받게 해요."
    OrbitCardType.WARP_GATE -> "서로 카드를 맞바꿔요."
    OrbitCardType.AI_CORE -> "효과는 없어요."
    OrbitCardType.CAPTAIN -> "가장 강한 카드. 내거나 버려지면 OUT돼요."
}
