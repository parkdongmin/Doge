package com.doge.simulator.presentation.screen.rank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.LeaderboardEntry
import com.doge.simulator.presentation.component.ContextualBottomBar
import com.doge.simulator.presentation.viewmodel.RankViewModel
import com.doge.simulator.ui.theme.*

@Composable
fun RankScreen(
    onHomeClick: () -> Unit = {},
    viewModel: RankViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val myRank by viewModel.myRank.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val myUid = viewModel.myUid

    LaunchedEffect(Unit) { viewModel.syncAndLoad() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 헤더 ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🏆 랭킹",
                    color = GoldAccent,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!isLoading) {
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("새로고침", color = SpaceAccent, style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SpaceAccent,
                        strokeWidth = 2.dp
                    )
                }
            }

            // ── 내 순위 배너 ──────────────────────────────────────
            myRank?.let { rank ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = GoldAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "나의 현재 순위",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${rank}위",
                            color = GoldAccent,
                            style = NumericMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading && entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SpaceAccent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("랭킹 불러오는 중...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("아직 랭킹 데이터가 없습니다", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("탐험 후 행성을 구매해 순위에 올려보세요!", color = TextDisabled, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(entries) { _, entry ->
                        RankEntryCard(
                            entry = entry,
                            isMe = entry.uid == myUid
                        )
                    }
                }
            }
        } // Column 끝

        // ── 컨텍스트 바텀 바 ──────────────────────────────────────
        ContextualBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onHomeClick = onHomeClick
        )
    } // Box 끝
}

@Composable
private fun RankEntryCard(
    entry: LeaderboardEntry,
    isMe: Boolean
) {
    val (rankIcon, rankColor) = when (entry.rank) {
        1 -> "🥇" to GoldAccent
        2 -> "🥈" to Color_Silver
        3 -> "🥉" to Color_Bronze
        else -> "#${entry.rank}" to TextSecondary
    }
    val borderColor = when {
        isMe -> GoldAccent.copy(alpha = 0.6f)
        entry.rank <= 3 -> rankColor.copy(alpha = 0.4f)
        else -> SpaceBlue
    }
    val bgColor = when {
        isMe -> GoldAccent.copy(alpha = 0.08f)
        entry.rank <= 3 -> rankColor.copy(alpha = 0.05f)
        else -> SpaceNavy
    }

    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 순위 ──────────────────────────────────────────────
            Text(
                text = rankIcon,
                color = rankColor,
                style = if (entry.rank <= 3) NumericMedium else NumericSmall,
                modifier = Modifier.widthIn(min = 48.dp)
            )

            // ── 이름 + 행성 수 ────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        color = if (isMe) GoldAccent else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = GoldAccent.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "나",
                                color = GoldAccent,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "🪐 ${entry.planetCount}개",
                    color = TextDisabled,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // ── 총 자산 ───────────────────────────────────────────
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAsset(entry.totalAsset),
                    color = if (isMe) GoldAccent else TextPrimary,
                    style = NumericSmall
                )
                Text(
                    text = "코인",
                    color = TextDisabled,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/** 1억 이상이면 억 단위로 줄여서 표시 */
private fun formatAsset(value: Long): String = when {
    value >= 100_000_000L -> "${"%.1f".format(value / 100_000_000.0)}억"
    value >= 10_000L -> "${"%.1f".format(value / 10_000.0)}만"
    else -> "%,d".format(value)
}

// 은·동 메달 전용 색상
private val Color_Silver = androidx.compose.ui.graphics.Color(0xFFB0BEC5)
private val Color_Bronze = androidx.compose.ui.graphics.Color(0xFFCD7F32)