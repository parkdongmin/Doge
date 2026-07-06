package com.doge.simulator.presentation.screen.expedition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.Expedition
import com.doge.simulator.domain.model.ExpeditionReport
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.model.StoryEvent
import com.doge.simulator.presentation.viewmodel.ExpeditionHistoryViewModel
import com.doge.simulator.ui.theme.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpeditionHistoryScreen(
    onBack: () -> Unit,
    viewModel: ExpeditionHistoryViewModel = hiltViewModel()
) {
    val activeExpeditions by viewModel.activeExpeditions.collectAsState()
    val astronauts by viewModel.astronauts.collectAsState()
    val allReports by viewModel.allReports.collectAsState()
    val unreadReports by viewModel.unreadReports.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("탐사 일지", color = GoldAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (unreadReports.isNotEmpty()) {
                            Text(
                                "보고서 ${unreadReports.size}건 대기 중",
                                color = StatusRed,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceNavy)
            )
        },
        containerColor = SpaceDark
    ) { padding ->
        val hasContent = activeExpeditions.isNotEmpty() || allReports.isNotEmpty()

        if (!hasContent) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📡", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("탐사 기록이 없습니다", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("탐사를 완료하면 이야기가 쌓입니다", color = TextDisabled, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── 진행 중인 탐사 ────────────────────────────────────
                if (activeExpeditions.isNotEmpty()) {
                    item {
                        Text(
                            "🚀  진행 중인 탐사",
                            color = SpaceAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(activeExpeditions, key = { "active_${it.id}" }) { expedition ->
                        ActiveExpeditionCard(expedition = expedition, astronauts = astronauts)
                    }
                    item { HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(vertical = 4.dp)) }
                }

                // ── 미확인 보고서 ─────────────────────────────────────
                val pending = allReports.filter { !it.isRead || it.hasPendingChoices }
                if (pending.isNotEmpty()) {
                    item {
                        Text(
                            "📬  미확인 보고서",
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(pending, key = { "pending_${it.expeditionId}" }) { report ->
                        ReportCard(
                            report = report,
                            isPending = true,
                            onChoose = { event, idx -> viewModel.choose(event, idx) },
                            onMarkRead = { viewModel.markAsRead(report.expeditionId) }
                        )
                    }
                    item { HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(vertical = 4.dp)) }
                }

                // ── 완료된 기록 ───────────────────────────────────────
                val completed = allReports.filter { it.isRead && !it.hasPendingChoices }
                if (completed.isNotEmpty()) {
                    item {
                        Text(
                            "📖  탐사 기록",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(completed, key = { "done_${it.expeditionId}" }) { report ->
                        ReportCard(report = report, isPending = false)
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
private fun ActiveExpeditionCard(expedition: Expedition, astronauts: List<Astronaut>) {
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
        shape = RoundedCornerShape(12.dp),
        color = SpaceNavy.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (isComplete) StatusGreen.copy(alpha = 0.7f) else SpaceBlue.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(expedition.category.icon, fontSize = 16.sp)
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "👥 ${teamNames.joinToString(", ")}",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (isComplete) StatusGreen else SpaceAccent,
                trackColor = SpaceMid
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${(progress * 100).toInt()}% 완료",
                color = TextDisabled,
                style = MaterialTheme.typography.labelSmall
            )
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

    val borderColor = when {
        report.isChapterEnding -> GoldAccent.copy(alpha = 0.7f)
        isPending -> SpaceAccent.copy(alpha = 0.6f)
        else -> SpaceMid.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SpaceNavy.copy(alpha = if (isPending) 0.95f else 0.7f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── 헤더: 기록 번호 + 챕터 ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        report.recordLabel,
                        color = if (report.isChapterEnding) GoldAccent else SpaceAccent,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SpaceBlue.copy(alpha = 0.3f)
                    ) {
                        Text(
                            "챕터 ${report.chapter} · ${report.chapterTitle}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (report.isChapterEnding) {
                    Text("⭐", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 기록 제목 ─────────────────────────────────────────────
            Text(
                report.recordTitle,
                color = if (report.isChapterEnding) GoldAccent else TextPrimary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (report.isChapterEnding) FontWeight.Bold else FontWeight.Normal
            )

            // ── 이벤트 카드들 ─────────────────────────────────────────
            if (report.events.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                report.events.forEach { event ->
                    EventCard(
                        event = event,
                        onChoose = if (event.isPending) { idx -> onChoose?.invoke(event, idx) } else null
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── 보고서 확인 버튼 (이벤트 없고 미확인인 경우) ─────────
            if (isPending && report.events.isEmpty() && !report.isRead) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onMarkRead?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SpaceBlue, contentColor = TextPrimary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("보고서 확인", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: StoryEvent,
    onChoose: ((Int) -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SpaceBlue.copy(alpha = 0.15f),
        border = BorderStroke(
            1.dp,
            if (event.isPending) SpaceAccent.copy(alpha = 0.5f) else SpaceMid.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 이벤트 제목
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(if (event.isPending) "❗" else "✅", fontSize = 12.sp)
                Text(
                    event.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                event.description,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall
            )

            if (event.isPending && onChoose != null) {
                // 선택지 버튼들
                Spacer(modifier = Modifier.height(10.dp))
                val choices = buildList {
                    add(Triple(0, event.choice1Label, event.choice1ResourceType to event.choice1Amount))
                    add(Triple(1, event.choice2Label, event.choice2ResourceType to event.choice2Amount))
                    if (event.choice3Label != null && event.choice3ResourceType != null && event.choice3Amount != null) {
                        add(Triple(2, event.choice3Label, event.choice3ResourceType to event.choice3Amount))
                    }
                }
                choices.forEach { (idx, label, reward) ->
                    val (resourceType, amount) = reward
                    OutlinedButton(
                        onClick = { onChoose(idx) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SpaceAccent.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                            if (amount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                Spacer(modifier = Modifier.height(6.dp))
                if (event.outcomeNote != null) {
                    Text(
                        event.outcomeNote,
                        color = StatusRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                } else {
                    val chosen = event.choiceAt(event.selectedChoiceIndex)
                    if (chosen != null) {
                        val (label, resourceType, amount) = chosen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("선택:", color = TextDisabled, style = MaterialTheme.typography.labelSmall)
                            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            if (amount > 0) {
                                Text("→", color = TextDisabled, style = MaterialTheme.typography.labelSmall)
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
