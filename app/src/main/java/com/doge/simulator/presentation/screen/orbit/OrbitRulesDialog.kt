package com.doge.simulator.presentation.screen.orbit

import androidx.compose.runtime.Composable
import com.doge.simulator.presentation.component.InfoDialog
import com.doge.simulator.presentation.component.InfoEntry

// 단계별 카드 해금 튜토리얼 대신, 기존 앱의 ⓘ 설명 다이얼로그 패턴을 그대로 재사용한다(FR-021).
// 처음부터 8종 카드를 전부 사용할 수 있다.
@Composable
fun OrbitRulesDialog(onDismiss: () -> Unit) {
    InfoDialog(title = "ORBIT 안내", onDismiss = onDismiss) {
        InfoEntry("기본 진행", "내 턴에 카드를 한 장 뽑아 두 장 중 하나를 내요. 상대를 OUT시키거나 덱이 다 떨어졌을 때 손패 Power가 높으면 라운드에서 이겨요.")
        InfoEntry("SIGNAL", "라운드에서 이기면 SIGNAL을 얻어요. 먼저 3개를 모으면 매치 승리예요.")
        InfoEntry("SCOUT DRONE (1)", "상대 카드의 Power를 하나 추측해요. 맞히면 상대는 즉시 OUT. Power 1은 지목할 수 없어요.")
        InfoEntry("SENSOR (2)", "상대가 가진 카드를 확인해요.")
        InfoEntry("PROBE (3)", "서로의 카드를 비교해 낮은 쪽이 OUT돼요. 같으면 아무 일도 없어요.")
        InfoEntry("SHIELD (4)", "다음 내 턴이 시작될 때까지 상대의 카드 효과를 전부 막아줘요.")
        InfoEntry("EMP (5)", "나 또는 상대 중 한 명이 카드를 버리고 새 카드를 받아요.")
        InfoEntry("WARP GATE (6)", "나와 상대의 카드를 서로 맞바꿔요.")
        InfoEntry("AI CORE (7)", "따로 효과는 없지만, EMP나 WARP GATE와 함께 손에 있으면 반드시 이 카드를 내야 해요.")
        InfoEntry("CAPTAIN (8)", "가장 강한 카드예요. 하지만 직접 내거나 EMP로 버려지면 즉시 OUT돼요.")
        InfoEntry("기억하기", "한 번 사용된 카드는 다시 확인할 수 없어요. 어떤 카드가 나왔는지는 직접 기억해야 해요.")
    }
}
