package com.doge.simulator.presentation.screen.rank

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
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

    RankScreenContent(
        entries = entries,
        myRank = myRank,
        isLoading = isLoading,
        myUid = myUid,
        onHomeClick = onHomeClick,
        onRefresh = { viewModel.refresh() }
    )
}

/** 서버 연동 없이 순수 UI만 렌더링 — 프리뷰·향후 테스트에서 재사용 */
@Composable
private fun RankScreenContent(
    entries: List<LeaderboardEntry>,
    myRank: Int?,
    isLoading: Boolean,
    myUid: String?,
    onHomeClick: () -> Unit,
    onRefresh: () -> Unit
) {
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
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_trophy),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                    )
                    Text(
                        text = "랭킹",
                        color = GoldAccent,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (!isLoading) {
                    TextButton(onClick = onRefresh) {
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
                        .padding(horizontal = Spacing.xl, vertical = Spacing.xs),
                    shape = RoundedCornerShape(6.dp),
                    color = GoldAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
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

            Spacer(modifier = Modifier.height(Spacing.sm))

            if (isLoading && entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SpaceAccent)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text("랭킹 불러오는 중...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_trophy),
                            contentDescription = null,
                            modifier = Modifier.size(IconGlyphSize.xlarge.value.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text("아직 랭킹 데이터가 없습니다", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("탐험 후 행성을 구매해 순위에 올려보세요!", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.xs))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
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
    val rankMedalRes = when (entry.rank) {
        1 -> R.drawable.ic_ui_medal_gold
        2 -> R.drawable.ic_ui_medal_silver
        3 -> R.drawable.ic_ui_medal_bronze
        else -> null
    }
    val rankColor = when (entry.rank) {
        1 -> GoldAccent
        2 -> Color_Silver
        3 -> Color_Bronze
        else -> TextSecondary
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
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 순위 ──────────────────────────────────────────────
            Box(modifier = Modifier.widthIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                if (rankMedalRes != null) {
                    Image(
                        painter = painterResource(rankMedalRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(
                        text = "#${entry.rank}",
                        color = rankColor,
                        style = NumericSmall
                    )
                }
            }

            // ── 이름 + 행성 수 ────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.displayName,
                        color = if (isMe) GoldAccent else TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = GoldAccent.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "나",
                                color = GoldAccent,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_planet),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${entry.planetCount}개",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
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
                    color = TextSecondary,
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

// ─── 프리뷰 전용: 실 서버(Firestore)에 데이터를 넣지 않고 화면만 미리 보기 위한 가짜 데이터 ───
private fun fakeLeaderboardEntries(count: Int): List<LeaderboardEntry> = List(count) { index ->
    val rank = index + 1
    LeaderboardEntry(
        uid = "preview_user_$rank",
        displayName = "도지플레이어$rank",
        totalAsset = (800_000_000L - rank * 12_000_000L).coerceAtLeast(30_000L),
        coins = (400_000_000L - rank * 6_000_000L).coerceAtLeast(10_000L),
        planetCount = (40 - rank / 2).coerceAtLeast(1),
        rank = rank
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF00020E, heightDp = 800)
@Composable
private fun RankScreenPreview() {
    val fakeEntries = fakeLeaderboardEntries(50)
    DogeTheme {
        RankScreenContent(
            entries = fakeEntries,
            myRank = 17,
            isLoading = false,
            myUid = "preview_user_17",
            onHomeClick = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00020E, heightDp = 800)
@Composable
private fun RankScreenPreview_TopRank() {
    val fakeEntries = fakeLeaderboardEntries(50)
    DogeTheme {
        RankScreenContent(
            entries = fakeEntries,
            myRank = 1,
            isLoading = false,
            myUid = "preview_user_1",
            onHomeClick = {},
            onRefresh = {}
        )
    }
}