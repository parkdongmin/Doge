package com.doge.simulator.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doge.simulator.ui.theme.*

// 가운데 확인 팝업 공용 틀 — 행성 매도·로그아웃·ⓘ 안내·강화·오프라인 수익 등 "짧게 묻고 끝나는" 팝업은 전부 이걸 쓴다.
// 머티리얼 기본 AlertDialog와 화면마다 직접 만든 Dialog가 섞여 제목·모서리·여백이 제각각이던 걸 통일(2026-10-02).
// 머리는 시설 창(FacilityPanel)과 같은 명판(GameNameplate), 아래는 GameDialogButtons.
// 본문은 넘치면 스크롤되지만 버튼 줄은 항상 아래에 고정.
// (내용이 많은 화면은 아래에서 올라오는 FacilityPanel 쪽 — 둘을 구분해서 쓴다.)
@Composable
fun GameDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color = TextSecondary,
    // 명판 오른쪽 보유 코인(베팅 창 등 코인을 고르는 팝업).
    coins: Long? = null,
    // 명판 오른쪽 ✕ — 버튼 줄만으로 닫을 수 있으면 생략.
    onClose: (() -> Unit)? = null,
    // 실수로 바깥을 눌러 닫히면 안 되는 팝업(오프라인 수익 등)은 false.
    dismissOnClickOutside: Boolean = true,
    // 결정적 순간 강조(강화 결과 공개 중 초록/빨강 등)에만 바꾼다.
    borderColor: Color = SpaceBlue,
    // 명판 바로 아래, 스크롤되지 않는 자리(탭 등). 본문을 내려도 위에 남는다.
    header: (@Composable () -> Unit)? = null,
    // 탭을 바꿀 때 맨 위로 되돌리는 등 본문 스크롤을 밖에서 다뤄야 할 때만 넘긴다.
    scrollState: ScrollState = rememberScrollState(),
    buttons: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissOnClickOutside)) {
        val shape = RoundedCornerShape(16.dp)
        // 폭 = min(화면 폭의 88%, 440dp). 예전엔 fillMaxWidth(0.88f).widthIn(max = 440.dp) 순서라 88%가 먼저
        // 고정돼 상한이 먹히지 않았고, 폴드를 펼치면 팝업이 화면을 꽉 채웠다(2026-10-02).
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier
                .width(minOf(maxWidth * 0.88f, DIALOG_MAX_WIDTH))
                .padding(vertical = Spacing.xxl)
                .clip(shape)
                .border(1.dp, borderColor, shape)
                .background(SpaceNavy)
        ) {
            GameNameplate(
                title = title,
                subtitle = subtitle,
                subtitleColor = subtitleColor,
                coins = coins,
                onInfo = null,
                infoDescription = "",
                onClose = onClose
            )
            if (header != null) {
                Box(Modifier.padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.lg)) { header() }
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
                    .padding(start = Spacing.xl, end = Spacing.xl, top = if (header != null) Spacing.md else Spacing.lg, bottom = Spacing.lg),
                content = content
            )
            if (buttons != null) {
                Box(Modifier.padding(start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl)) { buttons() }
            }
        }
        }
    }
}

private val DIALOG_MAX_WIDTH = 440.dp
