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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.ButtonDepth
import com.doge.simulator.ui.theme.ButtonPadding
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
                    "완료되면 알림으로 알려드릴게요. 어떤 탐사에서 뭐가 나오는지는 ⓘ 를 눌러 보세요.",
                buttonLabel = "알겠어요",
                target = TutorialTarget.EXPLORE_FIRST_CATEGORY,
                onPrimary = { onDismiss(step) }
            )
        }
        is TutorialStep.Part2 -> OverlaySpec(
            body = "'행성' 탭이에요. 방금 지급된 첫 행성이 여기 있습니다.\n\n" +
                "행성은 가만히 둬도 분당 코인을 생산해요. 앱을 꺼 둬도 쌓입니다. " +
                "가끔 악재가 생기면 생산이 흔들리기도 하니, 상태는 '소식' 탭에서 확인하세요.",
            buttonLabel = "확인",
            target = null,
            onPrimary = { onDismiss(step) }
        )
        is TutorialStep.HqIntro -> OverlaySpec(
            body = "'정거장'이에요. 여기서 대원을 더 고용하고, 우주선을 늘리거나 강화하고, " +
                "연구로 새 탐사 지역과 행성 슬롯을 엽니다.\n\n" +
                "탐사가 막히면 대부분 여기서 풀려요.",
            buttonLabel = "확인",
            target = null,
            onPrimary = { onDismiss(step) }
        )
        is TutorialStep.UpgradeIntro -> OverlaySpec(
            body = "행성 상세 화면이에요. '강화'하면 생산량이 오릅니다 — 코인·자원이 들어요.\n\n" +
                "레벨 ${GameConstants.DANGER_ZONE_START}부터는 강화 실패 위험이 커지니 무리하지 마세요. " +
                "필요 없어진 행성은 '매도'로 정리할 수 있어요.",
            buttonLabel = "확인",
            target = null,
            onPrimary = { onDismiss(step) }
        )
        is TutorialStep.None -> return
    }

    val holeRect: Rect? = targets.get(spec.target)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(step) { detectTapGestures { spec.onPrimary() } }
    ) {
        ScrimWithHole(holeRect)

        // 버블 위치: 뚫린 영역이 화면 아래쪽이면 버블을 위로, 아니면 아래로.
        val density = LocalDensity.current
        val anchorTop = holeRect != null && holeRect.center.y > with(density) { 400.dp.toPx() }

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
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
                border = ButtonDepth.highlightBorder,
                elevation = ButtonDepth.elevation(),
                contentPadding = ButtonPadding.fullWidthCta
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}
