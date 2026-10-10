package com.doge.simulator.presentation.component

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doge.simulator.R
import com.doge.simulator.ui.theme.*

// 정거장 시설 창 — 시설을 "새 페이지"로 넘기면 상단 바·뒤로가기가 갑자기 생겨 서비스 앱 같다는 피드백으로,
// 정거장 화면 위에 창을 띄우는 방식(하단 탭 유지, 위쪽으로 정거장이 살짝 보임). ✕·바깥 터치·뒤로가기로 닫는다.
// 창이 뒤 배경과 같은 남색이라 "창"으로 안 떠 보였다 — 막을 진하게,
// 명판은 본문보다 한 톤 밝게(SpaceMid) 해서 창 머리를 구분한다. 테두리는 금색으로 둘렀다가 강조색(금색)이
// 창 전체를 감싸 내용과 눈길을 다퉈서, 정거장 시설 카드와 같은 파랑(SpaceBlue) 1dp로.
@Composable
fun FacilityPanel(
    visible: Boolean,
    title: String,
    onClose: () -> Unit,
    coins: Long? = null,
    onInfo: (() -> Unit)? = null,
    infoDescription: String = "안내",
    // 제목 아래 작은 상태 한 줄(탐사 일지 "보고서 N건 대기 중" 등).
    subtitle: String? = null,
    subtitleColor: Color = TextSecondary,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(enabled = visible, onBack = onClose)

    Box(Modifier.fillMaxSize()) {
        // 뒤 정거장을 어둡게 — 누르면 닫힘.
        AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(SpaceDark.copy(alpha = 0.78f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = PANEL_TOP_GAP)
                    .clip(shape)
                    .border(1.dp, SpaceBlue, shape)
                    // 창 안쪽 클릭이 뒤 막(닫기)으로 새지 않게 막는다.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                NightSkyBackground(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        GameNameplate(title, subtitle, subtitleColor, coins, onInfo, infoDescription, onClose)
                        content()
                    }
                }
            }
        }
    }
}

// 창·팝업 공용 명판 — 이름(+ 상태 한 줄) + (코인) + (ⓘ) + (✕). 시설 창(FacilityPanel)과
// 가운데 확인 팝업(GameDialog)이 같이 써서 "창 머리" 모양을 하나로 맞춘다.
// 제목 옆 아이콘은 뺐다(2026-10-02) — 정거장 시설 카드 그림 등과 같은 그림이 화면에 또 나와 중복이 많았다.
@Composable
internal fun GameNameplate(
    title: String,
    subtitle: String?,
    subtitleColor: Color,
    coins: Long?,
    onInfo: (() -> Unit)?,
    infoDescription: String,
    onClose: (() -> Unit)?
) {
    Column(Modifier.textured(shape = RectangleShape, baseColor = SpaceMid)) {
        Row(
            Modifier
                .fillMaxWidth()
                // ✕·ⓘ가 없는 팝업도 명판 높이가 같게(아이콘 버튼 48dp 기준).
                .heightIn(min = 56.dp)
                .padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = GoldAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(subtitle, color = subtitleColor, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (coins != null) {
                Image(
                    painter = painterResource(R.drawable.ic_ui_coin),
                    contentDescription = "보유 코인",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text("%,d".format(coins), color = GoldAccent, style = NumericXSmall)
            }
            if (onInfo != null) {
                Spacer(Modifier.width(Spacing.sm))
                HelpChip(onClick = onInfo, contentDescription = infoDescription)
            } else {
                Spacer(Modifier.width(Spacing.sm))
            }
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(PixelIcons.Close, contentDescription = "닫기", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            } else {
                Spacer(Modifier.width(Spacing.sm))
            }
        }
        // 금색 반투명 선은 밝은 명판·어두운 본문 사이에서 붉게 보여서 테두리와 같은 파랑으로.
        HorizontalDivider(color = SpaceBlue)
    }
}

// 창 위로 정거장이 보이는 높이 — "정거장 안에서 시설 창을 열었다"는 느낌을 주는 부분.
private val PANEL_TOP_GAP = 72.dp
