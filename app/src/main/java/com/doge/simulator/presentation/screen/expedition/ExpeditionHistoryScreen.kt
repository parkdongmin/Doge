package com.doge.simulator.presentation.screen.expedition

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.BodyReading
import com.doge.simulator.ui.theme.LabelTiny
import com.doge.simulator.domain.model.StoryContent
import com.doge.simulator.domain.model.StoryReader
import com.doge.simulator.presentation.component.SegmentedTabs
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.coinSkipCost
import com.doge.simulator.presentation.component.DailyResetEffect
import com.doge.simulator.presentation.component.GameDialog
import com.doge.simulator.presentation.component.GameDialogButtons
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.domain.model.StoryProgress
import com.doge.simulator.domain.model.representativeIconRes
import com.doge.simulator.presentation.viewmodel.ExpeditionHistoryViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.presentation.component.PixelProgressBar
import com.doge.simulator.presentation.component.GameButton
import com.doge.simulator.presentation.component.GameButtonBox
import com.doge.simulator.presentation.component.GameButtonSize
import com.doge.simulator.presentation.component.GameButtonStyle
import com.doge.simulator.util.findActivity
import com.doge.simulator.presentation.component.FacilityPanel
import java.util.concurrent.TimeUnit

// 탐사 일지 — 탐험 탭 위에 띄우는 창(FacilityPanel). 기록을 훑어보고 닫는 화면이라 정거장 시설 창과 같은 방식.
// 명판 아이콘은 뺐다 — 책 아이콘이 SF 세계관에 비해 올드해 보였고, 제목만으로 충분하다.
@Composable
fun ExpeditionLogPanel(
    visible: Boolean,
    onClose: () -> Unit,
    viewModel: ExpeditionHistoryViewModel = hiltViewModel()
) {
    val activeExpeditions by viewModel.activeExpeditions.collectAsState()
    val astronauts by viewModel.astronauts.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val unreadReports by viewModel.unreadReports.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val storyProgress by viewModel.storyProgress.collectAsState()
    val skipAdsRemaining by viewModel.skipAdsRemaining.collectAsState()
    DailyResetEffect(onRefresh = viewModel::refreshSkipAdsRemaining)
    val coinSkipUnits by viewModel.coinSkipUnits.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val activity = LocalContext.current.findActivity()
    // 코인 단축은 금액이 클 수 있어 한 번 더 확인 — 확인 창에 본 금액보다 더 받지 않는다(유스케이스에서 보장)
    var pendingCoinSkip by remember { mutableStateOf<Pair<Expedition, Long>?>(null) }

    pendingCoinSkip?.let { (expedition, cost) ->
        GameDialog(
            title = "코인으로 바로 완료",
            onDismissRequest = { pendingCoinSkip = null },
            buttons = {
                GameDialogButtons(
                    confirmText = "완료",
                    onConfirm = {
                        viewModel.skipExpeditionWithCoins(expedition, cost)
                        pendingCoinSkip = null
                    },
                    confirmStyle = GameButtonStyle.Gold,
                    confirmEnabled = coins >= cost,
                    dismissText = "취소",
                    onDismiss = { pendingCoinSkip = null }
                )
            }
        ) {
            Text(
                "${"%,d".format(cost)}코인을 써서 바로 완료할까요?",
                color = TextSecondary,
                style = BodyReading
            )
        }
    }

    FacilityPanel(
        visible = visible,
        title = "탐사 일지",
        onClose = onClose,
        subtitle = if (unreadReports.isNotEmpty()) "보고서 ${unreadReports.size}건 대기 중" else null,
        subtitleColor = StatusRed
    ) {
        // 탭 2개 — "기록"은 진행 중인 탐사·보고서·선택지, "이야기"는 지금까지 본 이야기를 한 편의 글처럼 장별로 이어 읽기
        var tab by rememberSaveable { mutableIntStateOf(0) }
        SegmentedTabs(
            tabs = listOf("기록", "이야기"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md)
        )
        val progress = storyProgress
        val currentChapter = progress?.let { StoryReader.currentChapter(it) }
        // 지금 장에서 본 일반 기록 수 — 스토리 생성(GenerateExpeditionReportUseCase)과 같은 기준
        val seenInCurrentChapter = allReports.count {
            it.chapter == currentChapter && !it.isChapterEnding && !it.isStoryEnding
        }

        if (tab == 1) {
            StoryReadingView(
                chapters = progress?.let { StoryReader.chapters(it, seenInCurrentChapter) }.orEmpty(),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        } else {
            val hasContent = activeExpeditions.isNotEmpty() || allReports.isNotEmpty()

            if (!hasContent) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_no_signal),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text("탐사 기록이 없습니다", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("탐사를 완료하면 이야기가 쌓입니다", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    actionMessage?.let { msg ->
                        item {
                            Surface(shape = RoundedCornerShape(8.dp), color = SpaceBlue.copy(alpha = 0.3f)) {
                                Text(msg, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
                            }
                        }
                    }
                    // ── 진행 중인 탐사 ────────────────────────────────────
                    if (activeExpeditions.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.ic_ui_rocket),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconGlyphSize.large.value.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    "진행 중인 탐사",
                                    color = SpaceAccent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(activeExpeditions, key = { "active_${it.id}" }) { expedition ->
                            ActiveExpeditionCard(
                                expedition = expedition,
                                astronauts = astronauts,
                                skipAdsRemaining = skipAdsRemaining,
                                coinSkipUnit = coinSkipUnits[expedition.id],
                                coins = coins,
                                onSkipWaitAd = { viewModel.skipExpeditionWait(expedition, activity) },
                                onSkipWithCoins = { cost -> pendingCoinSkip = expedition to cost }
                            )
                        }
                        item { HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(vertical = Spacing.xs)) }
                    }

                    // ── 미확인 보고서 ─────────────────────────────────────
                    val pending = allReports.filter { !it.isRead || it.hasPendingChoices }
                    if (pending.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.ic_ui_mailbox),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconGlyphSize.large.value.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    "미확인 보고서",
                                    color = GoldAccent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        items(pending, key = { "pending_${it.expeditionId}" }) { report ->
                            ReportCard(
                                report = report,
                                isPending = true,
                                onChoose = { event, idx -> viewModel.choose(event, idx) },
                                onMarkRead = { viewModel.markAsRead(report.expeditionId) }
                            )
                        }
                        item { HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(vertical = Spacing.xs)) }
                    }

                    // ── 완료된 기록 ───────────────────────────────────────
                    val completed = allReports.filter { it.isRead && !it.hasPendingChoices }
                    // 다음 장 조건은 정말 막혔을 때(장 이야기를 다 봤는데 조건 미달)만 작게 보여 준다(2026-10-14) — 예전엔 장 내내
                    // "다음 챕터" 칸이 떠서 스토리보다 눈에 띄었고, 조건을 채운 뒤의 "남은 기록 N개"는 오해를 샀다
                    val stalledHint = progress?.let { StoryReader.stalledHint(it, seenInCurrentChapter) }
                    if (completed.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.ic_ui_logbook),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconGlyphSize.large.value.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    "탐사 기록",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        stalledHint?.let { hint -> item(key = "stalled_hint") { StalledHintRow(hint) } }
                        items(completed, key = { "done_${it.expeditionId}" }) { report ->
                            ReportCard(report = report, isPending = false)
                        }
                    }
                }
            }
        }
    }
}

// 정말 막혔을 때만(장 이야기를 다 봤는데 다음 장 조건 미달) 기록 목록 위에 작게 — "다음 챕터 조건" + 실제 조건.
// 이야기 말투 안내문(teaser)은 뺐다 — 막힘 문장이 이미 이야기로 말해 주고, 여긴 할 일만 또렷하면 된다
@Composable
private fun StalledHintRow(hint: StoryContent.NextStoryHint) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SpaceDark,
        border = BorderStroke(1.dp, SpaceMid.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_ui_lock),
                contentDescription = null,
                modifier = Modifier.size(IconGlyphSize.small.value.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "다음 챕터 조건",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.5.sp
                )
                Text(
                    hint.condition,
                    color = SpaceAccent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// "이야기" 탭 — 지금까지 본 이야기를 장별로 이어 붙인 글. 선택지·보상·막힘 문장 없이 줄거리만
@Composable
private fun StoryReadingView(chapters: List<StoryReader.Chapter>, modifier: Modifier = Modifier) {
    if (chapters.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("탐사를 완료하면 이야기가 쌓입니다", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        chapters.forEachIndexed { index, chapter ->
            if (index == 0 || chapters[index - 1].part != chapter.part) {
                item(key = "part_${chapter.part}") {
                    Text(
                        "${chapter.part}부",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item(key = "chapter_${chapter.number}") {
                Column {
                    Text(
                        "챕터 ${chapter.number} · ${chapter.title}",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(Spacing.md))
                    // 기록 하나가 한 문단 — 일지 문장이라 마침표가 없어 줄마다 끊어 읽히게 문단 간격을 둔다
                    chapter.lines.forEach { line ->
                        Text(line, color = TextPrimary.copy(alpha = 0.9f), style = BodyReading)
                        Spacer(Modifier.height(Spacing.sm))
                    }
                    chapter.ending?.let { ending ->
                        Spacer(Modifier.height(Spacing.xs))
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Image(
                                painter = painterResource(if (chapter.isPartEnding) R.drawable.ic_ui_trophy else R.drawable.ic_ui_star),
                                contentDescription = null,
                                modifier = Modifier.size(IconGlyphSize.small.value.dp)
                            )
                            Text(ending, color = TextPrimary, style = BodyReading, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "완료"
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        h > 0 -> "${h}시간 ${m}분"
        m > 0 -> "${m}분 ${s}초"
        else -> "${s}초"
    }
}

@Composable
private fun ActiveExpeditionCard(
    expedition: Expedition,
    astronauts: List<Astronaut>,
    skipAdsRemaining: Int,
    coinSkipUnit: Double?,
    coins: Long,
    onSkipWaitAd: () -> Unit,
    onSkipWithCoins: (cost: Long) -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() }
    }

    val remaining = (expedition.endTime - now).coerceAtLeast(0L)
    val total = expedition.endTime - expedition.startTime
    val progress = if (total > 0) ((now - expedition.startTime).toFloat() / total).coerceIn(0f, 1f) else 1f
    val isComplete = remaining <= 0L
    val teamNames = astronauts.filter { it.id in expedition.astronautIds }.map { it.name }

    Surface(
        modifier = Modifier.textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        // 진행 중 = 파랑(미확인 금색과 지난 기록 흐린 남색 사이), 완료 대기 = 초록.
        border = BorderStroke(1.dp, if (isComplete) StatusGreen.copy(alpha = 0.8f) else SpaceAccent.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Image(
                        painter = painterResource(expedition.category.representativeIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                    )
                    Text(
                        expedition.category.displayName,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(shape = RoundedCornerShape(4.dp), color = SpaceBlue.copy(alpha = 0.4f)) {
                        Text(
                            "T${expedition.tier}",
                            color = SpaceAccent,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                        )
                    }
                }
                Text(
                    if (isComplete) "완료 대기 중" else formatDuration(remaining),
                    color = if (isComplete) StatusGreen else TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isComplete) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (teamNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_crew),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                    )
                    Text(
                        teamNames.joinToString(", "),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            PixelProgressBar(
                progress = progress,
                color = if (isComplete) StatusGreen else SpaceAccent
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                "${(progress * 100).toInt()}% 완료",
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )

            // 행동 버튼은 정보(시간·대원·진행률)를 다 본 뒤 카드 맨 아래에 — 예전엔 남은 시간 줄 바로 밑에
            // 2dp 간격으로 붙어 있어 위아래가 답답했다.
            if (!isComplete && remaining > 60_000L) {
                Spacer(modifier = Modifier.height(Spacing.md))
                // 광고(하루 횟수 제한)와 코인 두 갈래 — 광고만 봐야 빨리 크는 구조라는 피드백 대응.
                // 카드당 강조색 버튼은 하나라 코인 쪽은 남색 + 금액만 금색
                val coinCost = coinSkipUnit?.let { coinSkipCost(remaining, it) }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    GameButton(
                        text = "광고 $skipAdsRemaining/${GameConstants.SKIP_WAIT_AD_DAILY_MAX}",
                        onClick = onSkipWaitAd,
                        enabled = skipAdsRemaining > 0,
                        modifier = Modifier.weight(1f),
                        style = GameButtonStyle.Primary,
                        leadingIcon = R.drawable.ic_ui_ad,
                        subText = when {
                            skipAdsRemaining <= 0 -> "오늘은 끝"
                            remaining <= GameConstants.AD_SKIP_MAX_MS -> "바로 완료"
                            else -> "4시간 당기기"
                        }
                    )
                    if (coinCost != null) {
                        GameButton(
                            text = "바로 완료",
                            onClick = { onSkipWithCoins(coinCost) },
                            enabled = coins >= coinCost,
                            modifier = Modifier.weight(1f),
                            style = GameButtonStyle.Neutral,
                            subCoinAmount = coinCost
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: ExpeditionReport,
    isPending: Boolean,
    onChoose: ((StoryEvent, Int) -> Unit)? = null,
    onMarkRead: (() -> Unit)? = null
) {
    // 이벤트가 있던 보고서는 모든 선택이 끝나면 자동으로 읅음 처리한다.
    // (이벤트가 처음부터 없던 보고서는 "보고서 확인" 버튼으로 직접 닫아야 한다)
    LaunchedEffect(report.expeditionId, report.hasPendingChoices, report.isRead) {
        if (isPending && report.events.isNotEmpty() && !report.hasPendingChoices && !report.isRead) {
            onMarkRead?.invoke()
        }
    }

    // 챕터 전환("완주" 포함)은 같은 골드 강조를 쓴다 — 완주는 그 위에 배지·문구만 다르게
    val isHighlighted = report.isChapterEnding || report.isStoryEnding
    // 화면 전체가 남색 일색이라 상태가 안 보였다(2026-10-02) — 눈이 가는 순서를
    // 금색(새로 온 미확인 보고서) > 파랑(진행 중 탐사) > 흐린 남색(지난 기록)으로 나눈다.
    val borderColor = when {
        isPending -> GoldAccent
        isHighlighted -> GoldAccent.copy(alpha = 0.4f)
        else -> SpaceMid.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        // 읽은 기록은 한 톤 어둡게 — 투명도로 낮추면 뒤 밤하늘 별이 카드 안에 비쳐 보여 불투명 색으로.
        color = if (isPending) SpaceNavy else lerp(SpaceDark, SpaceNavy, 0.6f),
        border = BorderStroke(if (isPending) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // ── 헤더: 기록 번호 + 챕터 ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (isPending) {
                        // 새로 온 보고서 표시 — 금색 바탕 작은 배지.
                        Surface(shape = RoundedCornerShape(4.dp), color = GoldAccent) {
                            Text(
                                "NEW",
                                color = SpaceDark,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
                            )
                        }
                    }
                    Text(
                        report.recordLabel,
                        color = when {
                            isPending || isHighlighted -> GoldAccent
                            else -> TextSecondary
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SpaceBlue.copy(alpha = 0.3f)
                    ) {
                        Text(
                            when {
                                // 완주 기록은 장 번호로 1부·2부를 가른다(1부 완주 = 5장, 2부 완주 = 10장)
                                report.isStoryEnding && report.chapter >= StoryProgress.LAST_CHAPTER -> "2부 완주"
                                report.isStoryEnding -> "1부 완주"
                                else -> "챕터 ${report.chapter} · ${report.chapterTitle}"
                            },
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                        )
                    }
                }
                if (isHighlighted) {
                    Image(
                        painter = painterResource(if (report.isStoryEnding) R.drawable.ic_ui_trophy else R.drawable.ic_ui_star),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // ── 기록 제목 ─────────────────────────────────────────────
            Text(
                report.recordTitle,
                color = when {
                    isHighlighted -> GoldAccent
                    isPending -> TextPrimary
                    else -> TextPrimary.copy(alpha = 0.75f) // 지난 기록은 한 단계 뒤로
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
            )

            // ── 이벤트 카드들 ─────────────────────────────────────────
            if (report.events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                report.events.forEach { event ->
                    EventCard(
                        event = event,
                        onChoose = if (event.isPending) { idx -> onChoose?.invoke(event, idx) } else null
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }

            // ── 보고서 확인 버튼 (이벤트 없고 미확인인 경우) ─────────
            if (isPending && report.events.isEmpty() && !report.isRead) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                GameButton(
                    text = "확인",
                    onClick = { onMarkRead?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    style = GameButtonStyle.Primary,
                    size = GameButtonSize.Large
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: StoryEvent,
    onChoose: ((Int) -> Unit)?
) {
    // 카드보다 한 톤 밝은 상자 + 카드 안쪽 폭을 꽉 채움 — 예전엔 카드와 거의 같은 색이라 상자가 잘 안 보였고,
    // 폭도 글 길이에 따라 제각각이라 줄이 안 맞았다. (카드 자체가 불투명이라 여기 투명도는 별이 비치지 않는다)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SpaceMid.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (event.isPending) SpaceAccent.copy(alpha = 0.7f) else SpaceBlue.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // 이벤트 제목
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Image(
                    painter = painterResource(if (event.isPending) R.drawable.ic_ui_pending else R.drawable.ic_ui_check),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    event.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                event.description,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )

            if (event.isPending && onChoose != null) {
                // 선택지 버튼들
                Spacer(modifier = Modifier.height(Spacing.md))
                val choices = buildList {
                    add(Triple(0, event.choice1Label, event.choice1ResourceType to event.choice1Amount))
                    add(Triple(1, event.choice2Label, event.choice2ResourceType to event.choice2Amount))
                    if (event.choice3Label != null && event.choice3ResourceType != null && event.choice3Amount != null) {
                        add(Triple(2, event.choice3Label, event.choice3ResourceType to event.choice3Amount))
                    }
                }
                choices.forEach { (idx, label, reward) ->
                    val (resourceType, amount) = reward
                    // 선택지는 우열 없는 보기들이라 남색(왼쪽 선택지 글자 + 오른쪽 보상).
                    // 큰 버튼(약 48dp) + 사이 8dp — 작게 붙어 있으면 위 선택지를 누르려다 아래 걸 누르기 쉬웠다.
                    GameButtonBox(
                        onClick = { onChoose(idx) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        style = GameButtonStyle.Neutral,
                        size = GameButtonSize.Large
                    ) { textColor, _ ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = textColor,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (amount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Image(
                                        painter = painterResource(resourceType.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        "+$amount",
                                        color = GoldAccent,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!event.isPending) {
                // 선택 완료 표시
                Spacer(modifier = Modifier.height(Spacing.sm))
                if (event.outcomeNote != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_danger),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            event.outcomeNote,
                            color = StatusRed,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    val chosen = event.choiceAt(event.selectedChoiceIndex)
                    if (chosen != null) {
                        val (label, resourceType, amount) = chosen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text("선택:", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            if (amount > 0) {
                                Text("→", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                Image(
                                    painter = painterResource(resourceType.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("+$amount", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
