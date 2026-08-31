package com.doge.simulator.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary

// ⓘ 아이콘으로 여는 설명 팝업 공용 껍데기. 폭은 화면의 88%로 잡아(고정 dp 아님) 폴더블·
// 태블릿 등 넓은 화면에서도 비율이 유지되도록 한다. 세로는 내용에 따라 늘고 넘치면 스크롤.
@Composable
fun InfoDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = Spacing.xxl),
            shape = RoundedCornerShape(16.dp),
            color = SpaceNavy,
            border = BorderStroke(1.dp, SpaceBlue)
        ) {
            Column(
                modifier = Modifier
                    .padding(Spacing.xl)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(title, color = GoldAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Spacing.md))
                content()
                Spacer(Modifier.height(Spacing.lg))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("확인", color = GoldAccent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoEntry(term: String, description: String) {
    Column(Modifier.padding(bottom = Spacing.sm)) {
        Text(term, color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.xxs))
        Text(description, color = TextSecondary, style = BodyReading)
    }
}

@Composable
private fun InfoSectionHeader(text: String) {
    Text(text, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(Spacing.xs))
}

// 우주선 스탯 설명 — 팀 빌더 · 격납고에서 공유.
@Composable
fun ColumnScope.ShipInfoContent() {
    InfoSectionHeader("우주선")
    InfoEntry("속도", "높을수록 탐사에 걸리는 시간이 줄어요.")
    InfoEntry("적재", "높을수록 한 번에 가져오는 자원 양이 늘어요.")
    InfoEntry("성공률", "탐사 성공 기본 확률. 격납고에서 우주선을 강화하면 올라요.")
    InfoEntry("탑승 인원", "태울 수 있는 대원 수. 강화로 늘어나고, 많이 태울수록 자원 획득량이 늘어요.")
}

// 우주인 설명 — 팀 빌더 · 우주인 센터에서 공유. showGrades는 우주인 센터에서만 true.
@Composable
fun ColumnScope.CrewInfoContent(showGrades: Boolean) {
    InfoSectionHeader("우주인")
    InfoEntry(
        "전문 분야",
        "대원마다 광물·행성·유적·외계 중 하나예요. 이번 탐사 종류와 같은 분야의 대원을 태우면 " +
            "성공률과 자원 획득량이 오릅니다. 숙련도가 높을수록 효과가 커요."
    )
    InfoEntry("숙련도", "훈련으로 올릴 수 있어요. 등급마다 올릴 수 있는 상한이 정해져 있습니다.")
    InfoEntry(
        "인원 수",
        "탑승 인원이 많을수록 자원 획득량이 늘어요. 성공률 보너스는 분야가 맞는 대원 중 숙련도가 가장 높은 1명만 반영돼요."
    )

    if (showGrades) {
        Spacer(Modifier.height(Spacing.sm))
        InfoSectionHeader("대원 등급")
        AstronautGrade.entries.forEach { grade ->
            InfoEntry(
                grade.displayName,
                "시작 숙련도 ${grade.startProficiencyRange.first}~${grade.startProficiencyRange.last} · 숙련도 상한 ${grade.proficiencyCap}"
            )
        }
        Text(
            "등급이 높을수록 모집 센터에 드물게 나오고 고용 비용도 비싸지만, 시작 숙련도와 상한이 높아요.",
            color = TextSecondary,
            style = BodyReading
        )
    }
}
