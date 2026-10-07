package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doge.simulator.domain.model.orbit.OrbitCardType
import com.doge.simulator.presentation.component.GameDialog
import com.doge.simulator.presentation.component.GameDialogButtons
import com.doge.simulator.presentation.component.InfoEntry
import com.doge.simulator.presentation.component.SegmentedTabs
import com.doge.simulator.ui.theme.*

// 단계별 카드 해금 튜토리얼 대신, 기존 앱의 ⓘ 설명 다이얼로그 패턴을 재사용한다(FR-021).
// 처음부터 8종 카드를 전부 사용할 수 있다.
//
// 한 화면에 11항목이 쭉 이어져 길다는 피드백으로 탭을 나눴다 — 처음 보는 사람은 "게임 방법"만 읽고
// 시작하고, 게임 중엔 "카드" 탭만 표처럼 훑어본다. 카드 탭은 한 줄씩만 — 세부 예외 규칙은
// 게임 중 어기려 할 때 경고로 안내된다.
// 카드별 장수는 OrbitCardType.count에서 바로 읽는다(CAPTAIN이 1장뿐인 걸 알아야 PROBE로 바로
// 승부를 거는 등 판단에 직접 쓰이고, 덱 구성을 바꿔도 안내가 어긋나지 않게).
@Composable
fun OrbitRulesDialog(onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    // 탭을 바꾸면 맨 위부터 — 카드 탭 아래쪽을 보다 넘어가면 짧은 탭의 빈 아래쪽이 보였을 것
    LaunchedEffect(tab) { scrollState.scrollTo(0) }
    GameDialog(
        title = "ORBIT 안내",
        onDismissRequest = onDismiss,
        // 탭은 스크롤 밖에 고정 — 작은 화면에서 카드 탭을 내려도 탭이 위로 사라지지 않게
        header = {
            SegmentedTabs(
                tabs = listOf("게임 방법", "카드"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )
        },
        scrollState = scrollState,
        buttons = { GameDialogButtons(confirmText = "확인", onConfirm = onDismiss) }
    ) {
        // 두 탭을 겹쳐 그리고 고른 쪽만 보이게 — 창 높이가 항상 긴 쪽에 맞춰져서 탭을 바꿔도
        // 창이 줄었다 늘었다 하며 움직이지 않는다. 숨은 쪽은 화면 낭독기에서도 빠지게 한다
        Box {
            TabPage(visible = tab == 0) { HowToPlay() }
            TabPage(visible = tab == 1) { OrbitCardType.entries.forEach { CardRuleRow(it) } }
        }
    }
}

@Composable
private fun TabPage(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = if (visible) Modifier else Modifier.alpha(0f).clearAndSetSemantics {},
        content = content
    )
}

@Composable
private fun HowToPlay() {
    val total = OrbitCardType.entries.sumOf { it.count }
    InfoEntry("기본 진행", "내 턴에 카드를 한 장 뽑아 두 장 중 하나를 내요. 상대를 OUT시키거나 덱이 다 떨어졌을 때 손패 Power가 높으면 라운드에서 이겨요.")
    InfoEntry("SIGNAL", "라운드에서 이기면 SIGNAL을 얻어요. 먼저 3개를 모으면 매치 승리예요.")
    InfoEntry("카드 구성", "라운드마다 카드 ${total}장을 새로 섞어 전부 써요. 카드별 장수는 '카드' 탭에서 볼 수 있어요.")
    InfoEntry("기억하기", "한 번 사용된 카드는 다시 확인할 수 없어요. 어떤 카드가 나왔는지는 직접 기억해야 해요.")
}

// 아이콘 · Power · 이름 · 장수 한 줄 + 효과 한 줄
@Composable
private fun CardRuleRow(type: OrbitCardType) {
    Row(Modifier.fillMaxWidth().padding(bottom = Spacing.md), verticalAlignment = Alignment.Top) {
        Image(
            painter = painterResource(cardIconRes(type)),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${type.power}", color = cardAccent(type), style = NumericSmall)
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    cardLabel(type),
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("×${type.count}", color = TextSecondary, style = NumericSmall)
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(cardRuleText(type), color = TextSecondary, style = BodyReading)
        }
    }
}

// 게임 화면과 같은 한 줄 설명 + 규칙 창에서만 덧붙이는 예외(게임 중엔 어기려 할 때 경고로 안내되는 것들)
private fun cardRuleText(type: OrbitCardType): String = when (type) {
    OrbitCardType.SCOUT_DRONE -> cardShortDescription(type) + " 1은 지목할 수 없어요."
    OrbitCardType.AI_CORE -> cardShortDescription(type) + " EMP·WARP GATE와 같이 들면 꼭 내야 해요."
    else -> cardShortDescription(type)
}
