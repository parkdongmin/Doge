package com.doge.simulator.presentation.screen.explore

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.doge.simulator.R
import com.doge.simulator.domain.model.*
import com.doge.simulator.presentation.viewmodel.ExploreViewModel
import com.doge.simulator.ui.theme.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToExpeditionHistory: () -> Unit = {},
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val activeExpeditions by viewModel.activeExpeditions.collectAsState()
    val researchLab by viewModel.researchLab.collectAsState()
    val ownedPlanets by viewModel.ownedPlanets.collectAsState()
    val astronauts by viewModel.astronauts.collectAsState()
    val spaceships by viewModel.spaceships.collectAsState()
    val busyShipIds by viewModel.busyShipIds.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val unreadCount by viewModel.unreadReportCount.collectAsState()
    val latestReport by viewModel.latestReport.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
    ) {
        // ── 상단 헤더: 코인 + 자원 ────────────────────────────────────
        TopHeader(coins = coins, resources = resources)

        // ── 탐험 슬롯 + 탐사 기록 카드 ──────────────────────────────
        SlotAndRecordCard(
            activeCount = activeExpeditions.size,
            totalSlots = spaceships.size.coerceAtLeast(1),
            unreadCount = unreadCount,
            latestReport = latestReport,
            onClick = onNavigateToExpeditionHistory
        )

        // ── 중앙 탐험 배경 이미지 ────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SpaceDark,
                            SpaceNavy.copy(alpha = 0.95f),
                            SpaceDark
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(com.doge.simulator.R.drawable.bg_explore),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }

        // ── 탐사 카테고리 카드 4개 ────────────────────────────────────
        Text(
            "탐험 종류 선택",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )
        CategoryGrid(
            researchLab = researchLab,
            onSelectCategory = { category ->
                viewModel.selectCategory(category)
                viewModel.openTeamBuilder()
            }
        )

        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    // ── 팀 빌더 바텀시트 ─────────────────────────────────────────
    if (uiState.isTeamBuilderOpen) {
        val busyShipIdsSnapshot by viewModel.busyShipIds.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeTeamBuilder() },
            containerColor = SpaceNavy,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 8.dp).size(40.dp, 4.dp),
                    shape = CircleShape, color = SpaceMid
                ) {}
            }
        ) {
            TeamBuilderContent(
                uiState = uiState,
                researchLab = researchLab,
                ownedPlanets = ownedPlanets,
                astronauts = astronauts,
                spaceships = spaceships,
                busyShipIds = busyShipIdsSnapshot,
                onSelectCategory = { viewModel.selectCategory(it) },
                onSelectTier = { viewModel.selectTier(it) },
                onToggleAstronaut = { viewModel.toggleAstronaut(it) },
                onSelectShip = { viewModel.selectSpaceship(it) },
                onDispatch = { viewModel.dispatch() },
                onDismiss = { viewModel.closeTeamBuilder() }
            )
        }
    }

    // ── 탐사 결과 다이얼로그 ─────────────────────────────────────
    uiState.completionResult?.let { result ->
        ExpeditionResultDialog(
            result = result,
            coins = coins,
            onBuyPlanet = { result.discoveredPlanet?.let { viewModel.buyDiscoveredPlanet(it) } },
            onDismiss = { viewModel.dismissResult() }
        )
    }
}

// ── 상단 헤더 ────────────────────────────────────────────────────────
@Composable
private fun TopHeader(coins: Long, resources: List<com.doge.simulator.domain.model.Resource>) {
    val resourceMap = remember(resources) { resources.associateBy { it.type } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "우주 탐사",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🪙", fontSize = 14.sp)
                Text(
                    "%,d".format(coins),
                    color = GoldAccent,
                    style = NumericSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ExpeditionCategory.entries.forEachIndexed { catIndex, category ->
                if (catIndex > 0) {
                    item(key = "sep_$catIndex") {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(SpaceMid.copy(alpha = 0.5f))
                        )
                    }
                }
                val categoryResources = ResourceType.entries.filter { it.category == category }
                items(categoryResources, key = { it.name }) { resType ->
                    val amount = resourceMap[resType]?.amount ?: 0L
                    val hasAmount = amount > 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(
                                SpaceNavy.copy(alpha = if (hasAmount) 0.6f else 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Image(
                            painter = painterResource(resType.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp).alpha(if (hasAmount) 1f else 0.35f)
                        )
                        Text(
                            amount.toString(),
                            color = if (hasAmount) TextSecondary else TextDisabled,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ── 슬롯 + 탐사 기록 카드 ─────────────────────────────────────────────
@Composable
private fun SlotAndRecordCard(
    activeCount: Int,
    totalSlots: Int,
    unreadCount: Int,
    latestReport: com.doge.simulator.domain.model.ExpeditionReport?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = SpaceNavy.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            if (unreadCount > 0) GoldAccent.copy(alpha = 0.6f) else SpaceBlue.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "탐험 대기 $activeCount/$totalSlots",
                        color = if (activeCount > 0) SpaceAccent else TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (unreadCount > 0) {
                        Surface(shape = RoundedCornerShape(4.dp), color = StatusRed.copy(alpha = 0.2f)) {
                            Text(
                                "보고서 ${unreadCount}건",
                                color = StatusRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (latestReport != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "${latestReport.recordLabel}  ${latestReport.recordTitle}",
                        color = if (latestReport.isChapterEnding) GoldAccent else TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "탐사 기록이 없습니다",
                        color = TextDisabled,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text("›", color = TextSecondary, fontSize = 18.sp)
        }
    }
}

// ── 행성 + 우주선 애니메이션 ─────────────────────────────────────────
@Composable
private fun PlanetHeroSection(
    modifier: Modifier = Modifier,
    storyProgress: com.doge.simulator.domain.model.StoryProgress =
        com.doge.simulator.domain.model.StoryProgress()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shipAngle"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // 챕터 뱃지
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SpaceNavy.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, SpaceBlue.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📖", fontSize = 11.sp)
                    Text(
                        "챕터 ${storyProgress.currentChapter}  ·  ${storyProgress.chapterTitle}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 행성 + 우주선 공전
            Box(contentAlignment = Alignment.Center) {
                // 행성 이미지
                Image(
                    painter = painterResource(com.doge.simulator.R.drawable.bg_planet_explore),
                    contentDescription = null,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                // 궤도 링 + 공전 우주선
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(240.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(0.5.dp, SpaceBlue.copy(alpha = 0.2f))
                    ) {}

                    val radians = Math.toRadians(angle.toDouble())
                    val orbitRadius = 120f
                    val offsetX = (orbitRadius * Math.cos(radians)).toFloat()
                    val offsetY = (orbitRadius * Math.sin(radians)).toFloat()

                    Box(
                        modifier = Modifier
                            .offset(offsetX.dp, offsetY.dp)
                            .rotate(angle + 90f)
                    ) {
                        Text("🚀", fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 탐사 완료 기록 수
            Text(
                "탐사 기록  ${storyProgress.totalRecordsCompleted}건",
                color = TextDisabled,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ── 카테고리 1줄 4개 ──────────────────────────────────────────────────
@Composable
private fun CategoryGrid(
    researchLab: ResearchLab,
    onSelectCategory: (ExpeditionCategory) -> Unit
) {
    val unlockedCategories = researchLab.unlockedCategories().toSet()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExpeditionCategory.entries.forEach { category ->
            CategoryCard(
                category = category,
                isUnlocked = category in unlockedCategories,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onSelectCategory(category) }
            )
        }
    }
}

private val categoryColors = mapOf(
    ExpeditionCategory.MINERAL to Color(0xFFE8A84C),           // 황금빛 주황 - 광석
    ExpeditionCategory.PLANET to Color(0xFF5DBF7A),             // 초록 - 생명/대기
    ExpeditionCategory.RUINS to Color(0xFFB07FE0),              // 보라 - 고대/신비
    ExpeditionCategory.ALIEN_CIVILIZATION to Color(0xFF4FC9E8)  // 시안 - 외계/미지
)

@Composable
private fun CategoryCard(
    category: ExpeditionCategory,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val identityColor = categoryColors[category] ?: TextPrimary
    // 카테고리 대표 자원 아이콘 (첫 번째)
    val representativeResource = ResourceType.entries.firstOrNull { it.category == category }
    val resourceCount = ResourceType.entries.count { it.category == category }

    Surface(
        modifier = modifier.clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isUnlocked) SpaceNavy else SpaceNavy.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (isUnlocked) identityColor.copy(alpha = 0.35f) else SpaceMid.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 상단: 카테고리 이름
            Text(
                category.displayName,
                color = if (isUnlocked) identityColor else TextDisabled,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            // 중간: 대표 자원 아이콘 (잠금이면 자물쇠) — 슬롯 높이를 고정해
            // 잠금·해금 카드의 전체 높이가 서로 달라지지 않게 한다
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (isUnlocked && representativeResource != null) {
                    Image(
                        painter = painterResource(representativeResource.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Text("🔒", fontSize = 26.sp)
                }
            }

            // 하단: 자원 종류 수 or 잠금 조건
            Text(
                if (isUnlocked) "${resourceCount}종 자원"
                else "Lv.${category.researchLevelRequired} 필요",
                color = if (isUnlocked) TextSecondary else TextDisabled,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

// ── 팀 빌더 콘텐츠 ──────────────────────────────────────────────────
@Composable
private fun TeamBuilderContent(
    uiState: ExploreUiState,
    researchLab: ResearchLab,
    ownedPlanets: List<com.doge.simulator.domain.model.Planet>,
    astronauts: List<com.doge.simulator.domain.model.Astronaut>,
    spaceships: List<Spaceship>,
    busyShipIds: Set<String> = emptySet(),
    onSelectCategory: (ExpeditionCategory) -> Unit,
    onSelectTier: (Int) -> Unit,
    onToggleAstronaut: (String) -> Unit,
    onSelectShip: (String) -> Unit,
    onDispatch: () -> Unit,
    onDismiss: () -> Unit
) {
    val unlockedCategories = researchLab.unlockedCategories()
    val idleAstronauts = astronauts.filter { it.status == AstronautStatus.IDLE }
    val selectedShip = spaceships.firstOrNull { it.id == uiState.selectedSpaceshipId }
    val maxCrew = selectedShip?.crewCapacity ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            "탐사 파견 설정", color = GoldAccent,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // ── 다음 해제 목표 배너 ────────────────────────────────────────
        val nextLockedTier = (1..10).firstOrNull { tier ->
            val cond = GameConstants.TIER_UNLOCK_CONDITIONS[tier] ?: return@firstOrNull false
            val count = ownedPlanets.count { planet ->
                val meta = PlanetMetaDataTable.data[planet.type]
                meta != null && meta.rarity.ordinal >= cond.requiredRarity.ordinal
            }
            count < cond.requiredCount
        }
        if (nextLockedTier != null) {
            val cond = GameConstants.TIER_UNLOCK_CONDITIONS[nextLockedTier]!!
            val currentCount = ownedPlanets.count { planet ->
                val meta = PlanetMetaDataTable.data[planet.type]
                meta != null && meta.rarity.ordinal >= cond.requiredRarity.ordinal
            }
            val locationName = GameConstants.TIER_LABELS[nextLockedTier] ?: "T$nextLockedTier"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = GoldAccent.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔓", fontSize = 18.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "다음 해제 목표: T$nextLockedTier $locationName",
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${cond.requiredRarity.name} 등급 이상 행성 ${cond.requiredCount}개 필요",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    // 진행도 뱃지
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentCount >= cond.requiredCount) StatusGreen.copy(alpha = 0.15f)
                                else SpaceMid.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "$currentCount / ${cond.requiredCount}",
                            color = if (currentCount >= cond.requiredCount) StatusGreen else TextDisabled,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = StatusGreen.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✅", fontSize = 16.sp)
                    Text(
                        text = "모든 탐사 지역 해제 완료!",
                        color = StatusGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("탐사 카테고리")
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            unlockedCategories.forEach { category ->
                val selected = uiState.selectedCategory == category
                Surface(
                    modifier = Modifier.clickable { onSelectCategory(category) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) SpaceBlue else SpaceNavy,
                    border = BorderStroke(1.dp, if (selected) SpaceAccent else SpaceMid)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(category.icon, fontSize = 18.sp)
                        Text(
                            category.displayName.replace(" 탐사", ""),
                            color = if (selected) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("탐사 지역 (티어)")
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items((1..10).toList()) { tier ->
                val condition = GameConstants.TIER_UNLOCK_CONDITIONS[tier]
                val isUnlocked = condition == null || run {
                    val count = ownedPlanets.count { planet ->
                        val meta = PlanetMetaDataTable.data[planet.type]
                        meta != null && meta.rarity.ordinal >= condition.requiredRarity.ordinal
                    }
                    count >= condition.requiredCount
                }
                val selected = uiState.selectedTier == tier && isUnlocked
                Surface(
                    modifier = Modifier.clickable(enabled = isUnlocked) { onSelectTier(tier) },
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        !isUnlocked -> SpaceNavy.copy(alpha = 0.35f)
                        selected    -> GoldAccent.copy(alpha = 0.2f)
                        else        -> SpaceNavy
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            !isUnlocked -> SpaceMid.copy(alpha = 0.3f)
                            selected    -> GoldAccent
                            else        -> SpaceMid
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            if (isUnlocked) "T$tier" else "🔒",
                            color = if (selected) GoldAccent else if (isUnlocked) TextSecondary else TextDisabled,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            GameConstants.TIER_LABELS[tier] ?: "",
                            color = if (isUnlocked) TextDisabled else TextDisabled.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp
                        )
                        if (!isUnlocked && condition != null) {
                            Text(
                                condition.label,
                                color = TextDisabled.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("우주선 선택")
        Spacer(modifier = Modifier.height(8.dp))
        if (spaceships.isEmpty()) {
            Text(
                "보유한 우주선이 없습니다. 본부 > 격납고에서 구매하세요.",
                color = StatusRed, style = MaterialTheme.typography.bodySmall
            )
        } else {
            spaceships.forEach { ship ->
                val isBusy = ship.id in busyShipIds
                val selected = uiState.selectedSpaceshipId == ship.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable(enabled = !isBusy) { onSelectShip(ship.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isBusy -> SpaceNavy.copy(alpha = 0.4f)
                        selected -> SpaceBlue
                        else -> SpaceNavy.copy(alpha = 0.7f)
                    },
                    border = BorderStroke(
                        1.dp, when {
                            isBusy -> SpaceMid.copy(alpha = 0.4f)
                            selected -> SpaceAccent
                            else -> SpaceMid
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(spaceshipImageRes(ship.crewCapacity)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${ship.name} (등급 ${ship.grade})",
                                color = if (isBusy) TextDisabled else TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isBusy) "탐사 중 — 복귀 후 사용 가능"
                                else "탑승 ${ship.crewCapacity}명 · 속도 ${ship.speed} · 적재 ${ship.cargo}",
                                color = if (isBusy) StatusRed.copy(alpha = 0.7f) else TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        when {
                            isBusy -> Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "탐사 중",
                                    color = StatusRed.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            selected -> Text("✓", color = SpaceAccent, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("우주인 선택 (${uiState.selectedAstronautIds.size}/${maxCrew}명)")
        Spacer(modifier = Modifier.height(8.dp))
        if (idleAstronauts.isEmpty()) {
            Text(
                "대기 중인 우주인이 없습니다. 본부 > 우주인 센터에서 고용하세요.",
                color = StatusRed, style = MaterialTheme.typography.bodySmall
            )
        } else {
            idleAstronauts.forEach { astronaut ->
                val selected = astronaut.id in uiState.selectedAstronautIds
                val canSelect = selected || uiState.selectedAstronautIds.size < maxCrew
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable(enabled = canSelect) { onToggleAstronaut(astronaut.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) SpaceBlue else SpaceNavy.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, if (selected) SpaceAccent else SpaceMid)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(astronaut.specialty.icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                astronaut.name, color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(astronaut.specialty.displayName, color = SpaceAccent, style = MaterialTheme.typography.labelSmall)
                                Text(astronaut.grade.displayName, color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                                Text("숙련도 ${astronaut.proficiency}", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (selected) Text("✓", color = SpaceAccent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        uiState.dispatchError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = StatusRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onDispatch,
            enabled = !uiState.isDispatching &&
                    uiState.selectedSpaceshipId != null &&
                    uiState.selectedAstronautIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                if (uiState.isDispatching) "파견 중..." else "🚀  탐사 파견",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── 탐사 결과 다이얼로그 ─────────────────────────────────────────────
@Composable
private fun ExpeditionResultDialog(
    result: ExpeditionCompletionResult,
    coins: Long,
    onBuyPlanet: () -> Unit,
    onDismiss: () -> Unit
) {
    val planet = result.discoveredPlanet
    val meta = planet?.let { PlanetMetaDataTable.data[it.type] }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            border = BorderStroke(1.dp, if (result.success) GoldAccent.copy(0.5f) else SpaceMid),
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (result.success) "✨ 탐사 성공!" else "😞 탐사 실패",
                    color = if (result.success) GoldAccent else TextSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (result.coinsEarned > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = GoldAccent.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💰 탐사 보상", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(
                                "+${"%,d".format(result.coinsEarned)} 코인",
                                color = GoldAccent,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (result.resources.isNotEmpty()) {
                    Text(
                        if (result.success) "획득 자원" else "위로 보상 (소량)",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    result.resources.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (type, amount) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SpaceBlue.copy(0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Image(
                                                painter = painterResource(type.iconRes),
                                                contentDescription = type.displayName,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(type.displayName, color = TextPrimary, style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text("×$amount", color = GoldAccent, style = NumericXSmall)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (planet != null && meta != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SpaceMid)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("🪐 새로운 행성 발견!", color = SpaceAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val imageUrl = meta.variants.firstOrNull { it.variantId == planet.variantId }?.imageUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).memoryCacheKey(imageUrl).build(),
                            contentDescription = meta.displayName,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(SpaceMid),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(meta.displayName, color = GoldAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(meta.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatChipMini("⚡ ${planet.production}/분", StatusGreen)
                        StatChipMini("💰 ${"%,d".format(planet.buyPrice)}", GoldAccent)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SpaceMid),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("닫기") }
                    if (planet != null && result.canBuyPlanet) {
                        Button(
                            onClick = { onBuyPlanet(); onDismiss() },
                            enabled = coins >= planet.buyPrice,
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark)
                        ) {
                            Text(
                                if (coins >= planet.buyPrice) "구매 ${"%,d".format(planet.buyPrice)}코인" else "코인 부족",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = GoldAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatChipMini(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

private fun spaceshipImageRes(crewCapacity: Int): Int = when {
    crewCapacity <= 3 -> R.drawable.spaceship_2
    crewCapacity <= 5 -> R.drawable.spaceship_4
    crewCapacity <= 7 -> R.drawable.spaceship_6
    else -> R.drawable.spaceship_8
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
