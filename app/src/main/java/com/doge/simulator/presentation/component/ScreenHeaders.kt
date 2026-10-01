package com.doge.simulator.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import com.doge.simulator.ui.theme.*

// 화면 공통 제목들. 탭·창을 오갈 때 같은 역할의 제목이 화면마다 크기·간격이 조금씩 달라 어수선해 보여서
// 한 곳으로 모았다(2026-10-01). 새 화면도 여기 것을 쓴다.

// 탭 화면 맨 위 페이지 제목(행성·정거장·자산) — 큰 제목 + 설명 한 줄, 오른쪽에 선택적으로 버튼 등.
// 바깥 여백은 TabHeaderPadding을 쓴다(부모가 이미 같은 좌우·위 여백을 주는 화면은 아래 여백만).
@Composable
fun TabHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    // 헤더 높이는 제목+설명만으로 정한다 — 오른쪽 요소(자산 탭 "설정" 버튼은 최소 터치 영역 때문에 더 크다)가
    // 높이에 끼어들면 줄이 늘어나 제목이 다른 탭보다 아래로 밀렸다. 오른쪽 요소는 아래 끝에 맞춰 두고,
    // 더 크면 위쪽 여백으로 삐져나가게 한다.
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            Column {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(Spacing.xs))
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Box { trailing?.invoke() }
        }
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val trailingPlaceable = measurables[1].measure(loose)
        val textPlaceable = measurables[0].measure(
            loose.copy(maxWidth = (constraints.maxWidth - trailingPlaceable.width).coerceAtLeast(0))
        )
        val height = textPlaceable.height
        layout(constraints.maxWidth, height) {
            textPlaceable.place(0, 0)
            trailingPlaceable.place(constraints.maxWidth - trailingPlaceable.width, height - trailingPlaceable.height)
        }
    }
}

// 탭 화면(탐험·행성·정거장·자산) 공통 좌우 여백 — 제목뿐 아니라 그 아래 목록·카드도 이 값으로 맞춘다.
val TabSideMargin = Spacing.xl

val TabHeaderPadding = PaddingValues(start = TabSideMargin, end = TabSideMargin, top = Spacing.lg, bottom = Spacing.md)

// 탭 화면 안 섹션 제목("보유 자원", "탐험 종류 선택" 등). 페이지 제목보다 확실히 작게.
@Composable
fun TabSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = modifier)
}

// 시설 창(FacilityPanel) 안 섹션 제목("모집 센터", "보유 우주선" 등). 오른쪽에 개수 등 보조 정보.
@Composable
fun PanelSectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            color = GoldAccent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(trailing, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}
