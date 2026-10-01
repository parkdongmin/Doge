package com.doge.simulator.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doge.simulator.ui.theme.*

// 세그먼트 탭 — 남색 막대 하나 안에 칸을 나란히 붙이고, 고른 칸만 게임 버튼(파랑 몸통 + 아래 두께)처럼 채운다.
// 머티리얼 TabRow(글자 + 파란 밑줄)가 웹·일반 앱 같다는 피드백으로 교체(2026-10-02, 행성 탭).
@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SpaceNavy)
            .border(1.dp, SpaceMid, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            if (index == selectedIndex) {
                GameButtonBox(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    style = GameButtonStyle.Primary,
                    size = GameButtonSize.Small
                ) { textColor, textShadow ->
                    Text(
                        title,
                        color = textColor,
                        style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else {
                // 고른 칸과 높이가 같게(게임 버튼 Small의 위아래 여백 + 아래 두께만큼).
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(GameButtonSize.Small.corner))
                        .clickable { onSelect(index) }
                        .padding(
                            top = GameButtonSize.Small.padV,
                            bottom = GameButtonSize.Small.padV + GameButtonSize.Small.lip
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = TextSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}
