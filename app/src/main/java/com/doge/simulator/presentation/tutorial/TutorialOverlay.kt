package com.doge.simulator.presentation.tutorial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Part2StoryContent
import com.doge.simulator.presentation.component.GameButton
import com.doge.simulator.presentation.component.GameButtonSize
import com.doge.simulator.presentation.component.GameButtonStyle
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.Spacing
import com.doge.simulator.ui.theme.TextPrimary

private const val SCRIM_ALPHA = 0.80f
private const val NPC_NAME = "관제탑"

/**
 * 첫 진입 튜토리얼 오버레이 — 화면 전체를 어둡게 덮고(스포트라이트 대상이 있으면 그 부분만
 * 밝게 뚫음) 하단(또는 상단)에 관제탑 AI 대사 버블을 띄운다. 아무 곳이나 탭하거나 버튼을
 * 누르면 [onPrimary]로 진행/종료. 실제 버튼을 조준하는 정교한 탭 통과는 하지 않는다 —
 * 안내를 읽고 닫은 뒤 플레이어가 직접 누른다("최소" 스코프).
 */
@Composable
fun TutorialOverlay(
    step: TutorialStep,
    targets: TutorialTargetRegistry,
    onAdvancePart1: () -> Unit,
    onAdvancePart2StoryIntro: () -> Unit,
    onDismiss: (TutorialStep) -> Unit,
) {
    if (step is TutorialStep.None) return

    val spec: OverlaySpec = when (step) {
        is TutorialStep.Part1 -> when (step.page) {
            0 -> OverlaySpec(
                body = "관제탑입니다. 우주인님, 첫 임무예요.\n\n" +
                    "정찰선과 대원, 그리고 첫 행성까지 준비해 뒀어요. " +
                    "행성으로 탐사대를 보내면 자원과 새 행성을 더 찾을 수 있습니다.",
                buttonLabel = "다음",
                target = null,
                onPrimary = onAdvancePart1
            )
            else -> OverlaySpec(
                body = "여기서 탐사할 종류를 고르세요.\n\n" +
                    "정찰선과 대원을 배치하고 파견하면 탐사가 시작됩니다. " +
                    "완료되면 알림으로 알려드릴게요. 어떤 탐사에서 뭐가 나오는지는 '도움말'을 눌러 보세요.",
                buttonLabel = "알겠어요",
                target = TutorialTarget.EXPLORE_FIRST_CATEGORY,
                onPrimary = { onDismiss(step) }
            )
        }
        is TutorialStep.Part2 -> OverlaySpec(
            body = "'행성' 탭이에요. 방금 지급된 첫 행성이 여기 있습니다.\n\n" +
                "행성은 가만히 둬도 분당 코인을 생산해요. 앱을 꺼 둬도 쌓입니다. " +
                "악재가 뜨면 생산이 줄고, 큰 악재면 행성이 고장 나서 정비할 때까지 오히려 코인이 줄어요. " +
                "행성 소식은 '소식' 탭에서 확인하세요.",
            buttonLabel = "확인",
            target = null,
            onPrimary = { onDismiss(step) }
        )
        is TutorialStep.HqIntro -> OverlaySpec(
            body = "'정거장'이에요. 여기서 대원을 더 고용하고, 우주선을 늘리거나 강화하고, " +
                "연구로 새 탐사 지역과 행성 슬롯을 엽니다. " +
                "휴게실에선 ORBIT 카드 게임에 코인을 걸 수 있어요. 이기면 불어나고, 지면 잃어요.\n\n" +
                "탐사가 막히면 대부분 여기서 풀려요.",
            buttonLabel = "확인",
            target = null,
            onPrimary = { onDismiss(step) }
        )
        is TutorialStep.UpgradeIntro -> OverlaySpec(
            body = "행성 상세 화면이에요. '강화'하면 생산량이 오릅니다. 코인·자원이 들어요.\n\n" +
                "레벨 ${GameConstants.DANGER_ZONE_START}부터는 강화 실패 위험이 커지니 무리하지 마세요. " +
                "필요 없어진 행성은 '매도'로 정리할 수 있어요.",
            buttonLabel = "확인",
            target = null,
            onPrimary = { onDismiss(step) }
        )
        // 2부 안내 5줄(story-content.md 원문) — 한 줄씩 넘기고, 마지막 줄에서 탐사 카테고리 카드를 비춘다.
        // 티어 칩은 카테고리를 눌러 여는 팀 구성 창 안에 있어 여기서 직접 비출 수 없다 — 창을 열면 T11이 강조된다
        is TutorialStep.Part2StoryIntro -> {
            val lines = Part2StoryContent.part2IntroLines
            val isLast = step.page >= lines.lastIndex
            OverlaySpec(
                body = lines[step.page.coerceIn(0, lines.lastIndex)],
                buttonLabel = if (isLast) "알겠어요" else "다음",
                target = if (isLast) TutorialTarget.EXPLORE_FIRST_CATEGORY else null,
                onPrimary = if (isLast) ({ onDismiss(step) }) else onAdvancePart2StoryIntro
            )
        }
        is TutorialStep.None -> return
    }

    // 타깃 좌표는 창(window) 기준인데 스크림은 이 오버레이 기준으로 그린다. 폴드 펼침처럼 넓은 화면에선
    // 앱 화면이 최대 폭 480dp로 가운데 정렬돼(MainActivity) 오버레이 원점이 창 원점보다 오른쪽에 있어,
    // 그대로 쓰면 노란 상자가 왼쪽 여백만큼 밀렸다(2026-10-12). 오버레이 좌표로 바꿔서 쓴다.
    var overlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val holeRect: Rect? = targets.get(spec.target)?.let { windowRect ->
        val coords = overlayCoords?.takeIf { it.isAttached } ?: return@let null
        Rect(coords.windowToLocal(windowRect.topLeft), coords.windowToLocal(windowRect.bottomRight))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayCoords = it }
            .pointerInput(step) { detectTapGestures { spec.onPrimary() } }
    ) {
        ScrimWithHole(holeRect)

        // 버블 위치: 뚫린 영역이 오버레이 아래쪽 절반이면 버블을 위로, 아니면 아래로.
        val overlayHeight = overlayCoords?.size?.height ?: 0
        val anchorTop = holeRect != null && holeRect.center.y > overlayHeight / 2f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(Spacing.lg),
            verticalArrangement = if (anchorTop) Arrangement.Top else Arrangement.Bottom
        ) {
            DialogueBubble(body = spec.body, buttonLabel = spec.buttonLabel, onClick = spec.onPrimary)
        }
    }
}

private data class OverlaySpec(
    val body: String,
    val buttonLabel: String,
    val target: TutorialTarget?,
    val onPrimary: () -> Unit,
)

@Composable
private fun ScrimWithHole(holeRect: Rect?) {
    val density = LocalDensity.current
    val pad = with(density) { 8.dp.toPx() }
    val corner = with(density) { 12.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (holeRect == null) {
            drawRect(color = Color.Black.copy(alpha = SCRIM_ALPHA))
            return@Canvas
        }
        val hole = Rect(
            left = (holeRect.left - pad).coerceAtLeast(0f),
            top = (holeRect.top - pad).coerceAtLeast(0f),
            right = (holeRect.right + pad).coerceAtMost(size.width),
            bottom = (holeRect.bottom + pad).coerceAtMost(size.height),
        )
        val scrim = Color.Black.copy(alpha = SCRIM_ALPHA)
        // 구멍을 남기고 상/하/좌/우 4개 밴드로 나눠 칠한다 (BlendMode 없이).
        drawRect(scrim, topLeft = Offset(0f, 0f), size = Size(size.width, hole.top))
        drawRect(scrim, topLeft = Offset(0f, hole.bottom), size = Size(size.width, size.height - hole.bottom))
        drawRect(scrim, topLeft = Offset(0f, hole.top), size = Size(hole.left, hole.height))
        drawRect(scrim, topLeft = Offset(hole.right, hole.top), size = Size(size.width - hole.right, hole.height))
        // 강조 테두리
        drawRoundRect(
            color = GoldAccent,
            topLeft = Offset(hole.left, hole.top),
            size = Size(hole.width, hole.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 2.dp.toPx() })
        )
    }
}

@Composable
private fun DialogueBubble(body: String, buttonLabel: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SpaceNavy,
        border = BorderStroke(1.dp, SpaceBlue),
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Surface(shape = RoundedCornerShape(4.dp), color = GoldAccent.copy(alpha = 0.15f)) {
                Text(
                    text = NPC_NAME,
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                )
            }
            Text(
                text = body,
                color = TextPrimary,
                style = BodyReading,
                modifier = Modifier.padding(top = Spacing.md)
            )
            GameButton(
                text = buttonLabel,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                style = GameButtonStyle.Primary,
                size = GameButtonSize.Large
            )
        }
    }
}
