package com.doge.simulator.presentation.component

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.doge.simulator.ui.theme.SpaceAccent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
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

// ⓘ 아이콘으로 여는 설명 팝업 — GameDialog 틀에 "확인" 버튼 하나.
@Composable
fun InfoDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    GameDialog(
        title = title,
        onDismissRequest = onDismiss,
        buttons = { GameDialogButtons(confirmText = "확인", onConfirm = onDismiss) },
        content = content
    )
}

// 화면을 처음 열었을 때 ⓘ 설명을 한 번 자동으로 띄운다 — 설명은 다 있는데 ⓘ를 눌러보지 않아
// "어떻게 하는 거지?" 반응이 많았다. active가 true가 되는 첫 순간에만(창이 실제로 열렸을 때) 띄우고 기록
@Composable
fun AutoShowInfoOnce(key: String, active: Boolean = true, show: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(key, active) {
        if (!active) return@LaunchedEffect
        val prefs = context.getSharedPreferences("info_auto_shown", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(key, false)) {
            prefs.edit { putBoolean(key, true) }
            show()
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
    InfoEntry("시간", "탐사에 걸리는 시간이 이만큼 줄어요. 격납고에서 강화하면 더 줄어요.")
    InfoEntry("자원", "기본 정찰선보다 한 번에 가져오는 자원이 이만큼 많아요. 강화하면 더 늘어요.")
    InfoEntry("성공률", "탐사 성공 확률이에요. 격납고에서 강화하면 올라요.")
    InfoEntry("탑승 인원", "태울 수 있는 대원 수예요. 강화로 늘고, 많이 태울수록 자원을 더 가져와요.")
}

// 우주인 설명 — 팀 빌더 · 우주인 센터에서 공유. showGrades는 우주인 센터에서만 true.
@Composable
fun ColumnScope.CrewInfoContent(showGrades: Boolean) {
    InfoSectionHeader("우주인")
    InfoEntry(
        "전문 분야",
        "대원마다 광물·행성·유적·외계 중 하나예요. 탐사 종류와 같은 분야의 대원을 태우면 " +
            "성공률과 자원 획득량이 올라요. 숙련도가 높을수록 효과가 커요."
    )
    InfoEntry("숙련도", "훈련으로 올릴 수 있어요. 등급마다 상한이 있어요.")
    InfoEntry(
        "인원 수",
        "많이 태울수록 자원을 더 가져와요. 분야가 맞는 대원은 숙련도가 전부 합산돼 자원 획득량에 " +
            "반영되지만(여러 명 태울수록 유리), 성공률 보너스만은 그중 숙련도가 가장 높은 1명만 적용돼요."
    )

    if (showGrades) {
        Spacer(Modifier.height(Spacing.sm))
        InfoSectionHeader("대원 등급")
        AstronautGrade.entries.forEach { grade ->
            InfoEntry(
                grade.displayName,
                "출현 ${grade.spawnPercent}% · 시작 숙련도 ${grade.startProficiencyRange.first}~${grade.startProficiencyRange.last} · 숙련도 상한 ${grade.proficiencyCap}"
            )
        }
        Text(
            "등급이 높을수록 모집 센터에 드물게 나오고 고용 비용도 비싸요. 대신 시작 숙련도와 상한이 높아요.",
            color = TextSecondary,
            style = BodyReading
        )
    }
}

// ⓘ 설명을 여는 버튼. 회색 아이콘만 있을 땐 남색 배경에 묻혀 거의 안 보였고 터치 영역도 아이콘 크기뿐이라,
// "도움말" 글자를 붙인 파란(안내 역할색) 테두리 버튼으로 통일 — 탐험·자산·행성 상세·시설 창·상단 바 공용
@Composable
fun HelpChip(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .clip(shape)
            .background(SpaceAccent.copy(alpha = 0.12f))
            .border(1.dp, SpaceAccent.copy(alpha = 0.5f), shape)
            .clickable(onClickLabel = contentDescription, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Icon(PixelIcons.Info, contentDescription = null, tint = SpaceAccent, modifier = Modifier.size(14.dp))
        Text("도움말", color = SpaceAccent, style = MaterialTheme.typography.labelSmall)
    }
}
