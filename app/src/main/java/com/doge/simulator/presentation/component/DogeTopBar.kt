package com.doge.simulator.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.doge.simulator.ui.theme.*

// 하위 화면(격납고·우주인 센터·연구소·휴게실·행성 상세·탐사 일지) 공용 상단 바.
// 머티리얼 TopAppBar에 단색만 칠한 기본 상태가 "샘플 앱" 같다는 피드백으로 통일:
// 픽셀 아이콘(PixelIcons) + 카드와 같은 textured 질감 + 아래 금색 경계선.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogeTopBar(
    title: @Composable () -> Unit,
    onBack: () -> Unit,
    onInfo: (() -> Unit)? = null,
    infoDescription: String = "안내"
) {
    Column {
        TopAppBar(
            title = title,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(PixelIcons.Back, contentDescription = "뒤로", tint = TextPrimary, modifier = Modifier.size(TOP_BAR_ICON_SIZE))
                }
            },
            actions = {
                if (onInfo != null) {
                    HelpChip(onClick = onInfo, contentDescription = infoDescription, modifier = Modifier.padding(end = Spacing.sm))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.textured(shape = RectangleShape, baseColor = SpaceNavy)
        )
        HorizontalDivider(color = GoldAccent.copy(alpha = 0.35f))
    }
}

@Composable
fun DogeTopBar(
    title: String,
    onBack: () -> Unit,
    onInfo: (() -> Unit)? = null,
    infoDescription: String = "안내"
) {
    DogeTopBar(
        title = { Text(title, color = GoldAccent, style = MaterialTheme.typography.titleMedium) },
        onBack = onBack,
        onInfo = onInfo,
        infoDescription = infoDescription
    )
}

// 픽셀 아이콘은 격자가 24dp 칸을 꽉 채워서(머티리얼 아이콘은 안쪽 여백이 있음) 기본 크기로 두면
// 너무 커 보였다 — 한 단계 줄인다.
private val TOP_BAR_ICON_SIZE = 18.dp
