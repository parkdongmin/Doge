package com.doge.simulator.presentation.screen.explore

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.doge.simulator.R
import com.doge.simulator.domain.model.*
import com.doge.simulator.presentation.viewmodel.ExploreViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity
import java.util.concurrent.TimeUnit
import kotlin.math.sin
import kotlin.random.Random

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
    val activity = LocalContext.current.findActivity()

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
            TwinklingStars(modifier = Modifier.matchParentSize())

            Image(
                painter = painterResource(com.doge.simulator.R.drawable.bg_explore),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = Spacing.sm)
            )
        }

        // ── 탐사 카테고리 카드 4개 ────────────────────────────────────
        Text(
            "탐험 종류 선택",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = Spacing.lg, bottom = Spacing.xs)
        )
        CategoryGrid(
            researchLab = researchLab,
            activeCountByCategory = activeExpeditions.groupingBy { it.category }.eachCount(),
            onSelectCategory = { category ->
                viewModel.selectCategory(category)
                viewModel.openTeamBuilder()
            }
        )
    }

    // ── 팀 빌더 바텀시트 ─────────────────────────────────────────
    if (uiState.isTeamBuilderOpen) {
        val busyShipIdsSnapshot by viewModel.busyShipIds.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeTeamBuilder() },
            containerColor = SpaceNavy,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = Spacing.sm).size(40.dp, 4.dp),
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
            onBuyPlanet = { result.discoveredPlanet?.let { viewModel.buyDiscoveredPlanet(it, activity) } },
            onDismiss = { if (result.isSlotFull) viewModel.convertSlotFullToCoin(activity) else viewModel.dismissResult(activity) },
            onOpenSwapPicker = { viewModel.openSwapPicker() }
        )
        if (uiState.isSwapPickerOpen && result.discoveredPlanet != null) {
            val discovered = result.discoveredPlanet
            SwapPickerDialog(
                discoveredPlanet = discovered,
                discoveredPlanetMeta = PlanetMetaDataTable.data[discovered.type],
                coins = coins,
                hasFreeSlot = ownedPlanets.size < researchLab.maxPlanetSlots,
                ownedPlanets = ownedPlanets,
                onSellOwned = { viewModel.sellOwnedPlanet(it) },
                onBuyDiscovered = { viewModel.buyDiscoveredPlanet(discovered, activity) },
                onConvertToCoin = { viewModel.convertSlotFullToCoin(activity) },
                onDismiss = { viewModel.closeSwapPicker() }
            )
        }
    }
}

// ── 상단 헤더 ────────────────────────────────────────────────────────
@Composable
private fun TopHeader(coins: Long, resources: List<com.doge.simulator.domain.model.Resource>) {
    val resourceMap = remember(resources) { resources.associateBy { it.type } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Image(
                    painter = painterResource(R.drawable.ic_ui_coin),
                    contentDescription = null,
                    modifier = Modifier.size(IconGlyphSize.small.value.dp)
                )
                Text(
                    "%,d".format(coins),
                    color = GoldAccent,
                    style = NumericSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier
                            .background(
                                SpaceNavy.copy(alpha = if (hasAmount) 0.6f else 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    ) {
                        Image(
                            painter = painterResource(resType.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp).alpha(if (hasAmount) 1f else 0.35f)
                        )
                        Text(
                            amount.toString(),
                            color = if (hasAmount) TextSecondary else TextDisabled,
                            style = LabelTiny
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
            .padding(horizontal = Spacing.lg)
            .clickable(onClick = onClick)
            .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (unreadCount > 0) GoldAccent.copy(alpha = 0.6f) else SpaceBlue.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
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
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                            )
                        }
                    }
                }
                if (latestReport != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        "${latestReport.recordLabel}  ${latestReport.recordTitle}",
                        color = if (latestReport.isChapterEnding) GoldAccent else TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        "탐사 기록이 없습니다",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text("›", color = TextSecondary, fontSize = IconGlyphSize.medium)
        }
    }
}

// ── 반짝이는 별 배경 ──────────────────────────────────────────────────
private data class StarSpec(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val phase: Float,
    val speed: Float
)

@Composable
private fun TwinklingStars(modifier: Modifier = Modifier, count: Int = 24) {
    val stars = remember {
        List(count) {
            StarSpec(
                xFraction = Random.nextFloat(),
                yFraction = Random.nextFloat(),
                radiusDp = Random.nextFloat() * 1.2f + 0.6f,
                phase = Random.nextFloat() * (2f * Math.PI.toFloat()),
                speed = Random.nextFloat() * 0.8f + 0.6f
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "starTime"
    )

    Canvas(modifier = modifier) {
        stars.forEach { star ->
            val alpha = 0.25f + 0.6f * ((sin(time * star.speed + star.phase) + 1f) / 2f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.radiusDp.dp.toPx(),
                center = Offset(size.width * star.xFraction, size.height * star.yFraction)
            )
        }
    }
}

// ── 카테고리 1줄 4개 ──────────────────────────────────────────────────
@Composable
private fun CategoryGrid(
    researchLab: ResearchLab,
    activeCountByCategory: Map<ExpeditionCategory, Int>,
    onSelectCategory: (ExpeditionCategory) -> Unit
) {
    val unlockedCategories = researchLab.unlockedCategories().toSet()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        ExpeditionCategory.entries.forEach { category ->
            CategoryCard(
                category = category,
                isUnlocked = category in unlockedCategories,
                activeCount = activeCountByCategory[category] ?: 0,
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
    activeCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val identityColor = categoryColors[category] ?: TextPrimary
    // 카테고리 대표 자원 아이콘 (첫 번째)
    val representativeResource = ResourceType.entries.firstOrNull { it.category == category }
    val resourceCount = ResourceType.entries.count { it.category == category }
    val isActive = isUnlocked && activeCount > 0

    Surface(
        modifier = modifier.clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = when {
            !isUnlocked -> SpaceNavy.copy(alpha = 0.45f)
            isActive    -> identityColor.copy(alpha = 0.12f)
            else        -> SpaceNavy
        },
        border = BorderStroke(
            if (isActive) 1.5.dp else 1.dp,
            when {
                !isUnlocked -> SpaceMid.copy(alpha = 0.25f)
                isActive    -> identityColor
                else        -> identityColor.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.sm, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단: 카테고리 이름
            Text(
                category.displayName,
                color = if (isUnlocked) identityColor else TextDisabled,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(Spacing.md))

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
                    Image(
                        painter = painterResource(R.drawable.ic_ui_lock),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 하단: 탐사중인 팀 수 > 자원 종류 수 > 잠금 조건 순으로 표시
            if (isActive) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_rocket),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                    )
                    Text(
                        "${activeCount}팀 탐사중",
                        color = identityColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else Text(
                if (isUnlocked) "${resourceCount}종 자원" else "Lv.${category.researchLevelRequired} 필요",
                color = if (isUnlocked) TextSecondary else TextDisabled,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            // 진행중 표시 점 3개 — 항상 슬롯을 예약해 카드 높이가 흔들리지 않게 함 (활성/비활성 모두 동일 구조)
            Box(modifier = Modifier.height(4.dp), contentAlignment = Alignment.Center) {
                if (isActive) {
                    ActivityDots(color = identityColor)
                }
            }
        }
    }
}

// ── 로딩 표시기처럼 순서대로 밝아지는 점 3개 ─────────────────────────
@Composable
private fun ActivityDots(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "activityDots")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing)
        ),
        label = "activityDotsTime"
    )

    Canvas(modifier = modifier.size(width = 18.dp, height = 4.dp)) {
        val dotRadius = 1.5.dp.toPx()
        val spacing = 6.dp.toPx()
        repeat(3) { index ->
            val phase = index * (2f * Math.PI.toFloat() / 3f)
            val alpha = 0.2f + 0.8f * ((sin(time - phase) + 1f) / 2f)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(spacing * index + dotRadius, size.height / 2f)
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
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm)
    ) {
        Text(
            "탐사 파견 설정", color = GoldAccent,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.md))

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
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_lock),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "다음 해제 목표: T$nextLockedTier $locationName",
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
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
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
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
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_check),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                    )
                    Text(
                        text = "모든 탐사 지역 해제 완료!",
                        color = StatusGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        SectionLabel("탐사 카테고리")
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            unlockedCategories.forEach { category ->
                val selected = uiState.selectedCategory == category
                Surface(
                    modifier = Modifier.clickable { onSelectCategory(category) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) SpaceBlue else SpaceNavy,
                    border = BorderStroke(1.dp, if (selected) SpaceAccent else SpaceMid)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(category.representativeIconRes),
                            contentDescription = null,
                            modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                        )
                        Text(
                            category.displayName.replace(" 탐사", ""),
                            color = if (selected) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        SectionLabel("탐사 지역 (티어)")
        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
                        selected    -> SpaceBlue
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
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        if (isUnlocked) {
                            Text(
                                "T$tier",
                                color = if (selected) GoldAccent else TextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_ui_lock),
                                contentDescription = null,
                                modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                            )
                        }
                        Text(
                            GameConstants.TIER_LABELS[tier] ?: "",
                            color = if (isUnlocked) TextSecondary else TextDisabled.copy(alpha = 0.5f),
                            style = LabelTiny
                        )
                        if (!isUnlocked && condition != null) {
                            Text(
                                condition.label,
                                color = TextDisabled.copy(alpha = 0.6f),
                                style = LabelTiny
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        SectionLabel("우주선 선택")
        Spacer(modifier = Modifier.height(Spacing.sm))
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
                        .padding(vertical = Spacing.xs)
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
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(spaceshipImageRes(ship.crewCapacity)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${ship.name} (등급 ${ship.grade})",
                                color = if (isBusy) TextDisabled else TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Spacing.xxs))
                            Text(
                                if (isBusy) "탐사 중 — 복귀 후 사용 가능"
                                else "탑승 ${ship.crewCapacity}명 · 속도 ${ship.speed} · 적재 ${ship.cargo}",
                                color = if (isBusy) StatusRed.copy(alpha = 0.7f) else TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (isBusy) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "탐사 중",
                                    color = StatusRed.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        SectionLabel("우주인 선택 (${uiState.selectedAstronautIds.size}/${maxCrew}명)")
        Spacer(modifier = Modifier.height(Spacing.sm))
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
                        .padding(vertical = Spacing.xs)
                        .clickable(enabled = canSelect) { onToggleAstronaut(astronaut.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) SpaceBlue else SpaceNavy.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, if (selected) SpaceAccent else SpaceMid)
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(astronaut.specialty.characterImageRes(astronaut.grade)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                astronaut.name, color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Spacing.xxs))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                Text(astronaut.specialty.displayName, color = SpaceAccent, style = MaterialTheme.typography.labelSmall)
                                Text(astronaut.grade.displayName, color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                                Text("숙련도 ${astronaut.proficiency}", color = GoldAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        uiState.dispatchError?.let {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(it, color = StatusRed, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        Button(
            onClick = onDispatch,
            enabled = !uiState.isDispatching &&
                    uiState.selectedSpaceshipId != null &&
                    uiState.selectedAstronautIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
            border = ButtonDepth.highlightBorder,
            elevation = ButtonDepth.elevation(),
            contentPadding = ButtonPadding.fullWidthCta
        ) {
            if (uiState.isDispatching) {
                Text(
                    "파견 중...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_rocket),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                    )
                    Text(
                        "탐사 파견",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

// ── 탐사 결과 다이얼로그 ─────────────────────────────────────────────
@Composable
private fun ExpeditionResultDialog(
    result: ExpeditionCompletionResult,
    coins: Long,
    onBuyPlanet: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSwapPicker: () -> Unit
) {
    val planet = result.discoveredPlanet
    val meta = planet?.let { PlanetMetaDataTable.data[it.type] }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            border = BorderStroke(1.dp, if (result.success) GoldAccent.copy(0.5f) else SpaceMid),
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Image(
                        painter = painterResource(if (result.success) R.drawable.ic_ui_success else R.drawable.ic_ui_fail),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.large.value.dp)
                    )
                    Text(
                        if (result.success) "탐사 성공!" else "탐사 실패",
                        color = if (result.success) GoldAccent else TextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.lg))

                if (result.coinsEarned > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = GoldAccent.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Image(
                                    painter = painterResource(R.drawable.ic_ui_coin),
                                    contentDescription = null,
                                    modifier = Modifier.size(IconGlyphSize.small.value.dp)
                                )
                                Text("탐사 보상", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                "+${"%,d".format(result.coinsEarned)} 코인",
                                color = GoldAccent,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                }

                if (result.resources.isNotEmpty()) {
                    Text(
                        if (result.success) "획득 자원" else "위로 보상 (소량)",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    result.resources.entries.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            row.forEach { (type, amount) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = SpaceBlue.copy(0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
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
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }

                if (planet != null && meta != null) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    HorizontalDivider(color = SpaceMid)
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_planet),
                            contentDescription = null,
                            modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                        )
                        Text(
                            "행성 발견!",
                            color = SpaceAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    val imageUrl = meta.variants.firstOrNull { it.variantId == planet.variantId }?.imageUrl
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(imageUrl).memoryCacheKey(imageUrl).build(),
                            contentDescription = meta.displayName,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)).background(SpaceMid),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.None
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    Text(
                        "${meta.displayName} #${planet.variantId.substringAfterLast("-")}",
                        color = GoldAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold
                    )
                    Text(meta.description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = Spacing.xs))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        StatChipMini("${planet.production}/분", StatusGreen, iconRes = R.drawable.ic_ui_energy)
                        StatChipMini("%,d".format(planet.buyPrice), GoldAccent, iconRes = R.drawable.ic_ui_coin)
                        if (result.isDuplicateVariant) {
                            StatChipMini("도감 등록됨", TextSecondary, iconRes = R.drawable.ic_ui_logbook)
                        } else {
                            StatChipMini("도감 미등록", SpaceAccent, iconRes = R.drawable.ic_ui_new)
                        }
                    }

                    if (result.isCurrentlyOwned) {
                        Text(
                            "이미 보유 중인 행성이에요. 능력치가 다를 수 있으니 비교해보고 구매하세요",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = Spacing.sm)
                        )
                    }

                    if (result.isSlotFull) {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = SpaceBlue.copy(alpha = 0.25f)
                        ) {
                            Text(
                                "행성 슬롯이 가득 찼어요. 그냥 닫으면 " +
                                    (if (result.isDuplicateVariant) "도감엔 이미 있으니 " else "도감 등록 + ") +
                                    "${"%,d".format(result.slotFullCoins)}코인, 아니면 보유 행성을 팔고 대신 가질 수 있어요",
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            )
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        OutlinedButton(
                            onClick = onOpenSwapPicker,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SpaceAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceAccent)
                        ) { Text("보유 행성 팔고 구매하기", fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SpaceMid),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text(if (result.isSlotFull) "코인으로 받기" else "닫기") }
                    if (planet != null && result.canBuyPlanet) {
                        Button(
                            onClick = { onBuyPlanet(); onDismiss() },
                            enabled = coins >= planet.buyPrice,
                            modifier = Modifier.weight(2f).widthIn(min = ButtonPadding.minWidth),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
                            border = ButtonDepth.highlightBorder,
                            elevation = ButtonDepth.elevation()
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

// ── 슬롯 풀 상태에서 보유 행성을 팔아 자리를 만들고, 새로 발견한 행성을 구매할지 고르는 선택창 ──────
@Composable
private fun SwapPickerDialog(
    discoveredPlanet: Planet,
    discoveredPlanetMeta: PlanetMetaData?,
    coins: Long,
    hasFreeSlot: Boolean,
    ownedPlanets: List<Planet>,
    onSellOwned: (planetId: String) -> Unit,
    onBuyDiscovered: () -> Unit,
    onConvertToCoin: () -> Unit,
    onDismiss: () -> Unit
) {
    val sorted = remember(ownedPlanets) {
        ownedPlanets.sortedBy { it.buyPrice + it.upgradeInvestment }
    }
    val discoveredName = "${discoveredPlanetMeta?.displayName ?: discoveredPlanet.type.name} #${discoveredPlanet.variantId.substringAfterLast("-")}"
    val canBuyNow = hasFreeSlot && coins >= discoveredPlanet.buyPrice
    // 실수로 잘못 눌러 값비싼 행성을 파는 걸 막기 위한 매도 확인 단계
    var pendingSell by remember { mutableStateOf<Planet?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpaceNavy),
            border = BorderStroke(1.dp, SpaceMid),
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(Spacing.xxl)) {
                Text(
                    "$discoveredName 대신 팔 행성을 고르세요",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "판매가는 매입가 + 강화 투자액의 95%예요. 여러 개를 팔아 코인을 모은 뒤 구매하기를 눌러도 돼요",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.md)
                )
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (sorted.isEmpty()) {
                        Text("팔 수 있는 보유 행성이 없어요", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                    sorted.forEach { owned ->
                        val meta = PlanetMetaDataTable.data[owned.type]
                        val sellPrice = ((owned.buyPrice + owned.upgradeInvestment) * (1 - GameConstants.SELL_FEE_RATE)).toLong()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SpaceMid.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, SpaceMid)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${meta?.displayName ?: owned.type.name} #${owned.variantId.substringAfterLast("-")} Lv.${owned.level}",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(Spacing.xxs))
                                    Text(
                                        "판매가 ${"%,d".format(sellPrice)}코인",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Button(
                                    onClick = { pendingSell = owned },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed, contentColor = TextPrimary),
                                    border = ButtonDepth.highlightBorder,
                                    elevation = ButtonDepth.elevation(),
                                    contentPadding = ButtonPadding.listItemAction
                                ) { Text("매도", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
                HorizontalDivider(color = SpaceMid)
                Spacer(modifier = Modifier.height(Spacing.md))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(
                        onClick = onConvertToCoin,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SpaceMid),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) { Text("코인으로 받기", style = MaterialTheme.typography.labelSmall) }
                    Button(
                        onClick = onBuyDiscovered,
                        enabled = canBuyNow,
                        modifier = Modifier.weight(1f).widthIn(min = ButtonPadding.minWidth),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
                        border = ButtonDepth.highlightBorder,
                        elevation = ButtonDepth.elevation()
                    ) {
                        Text(
                            if (!hasFreeSlot) "슬롯 필요" else if (!canBuyNow) "코인 부족" else "구매하기",
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SpaceMid),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) { Text("취소 (탐사 결과로 돌아가기)", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }

    pendingSell?.let { target ->
        val meta = PlanetMetaDataTable.data[target.type]
        val sellPrice = ((target.buyPrice + target.upgradeInvestment) * (1 - GameConstants.SELL_FEE_RATE)).toLong()
        AlertDialog(
            onDismissRequest = { pendingSell = null },
            title = { Text("정말 파시겠어요?") },
            text = {
                Text("${meta?.displayName ?: target.type.name} #${target.variantId.substringAfterLast("-")} Lv.${target.level}을 " +
                    "${"%,d".format(sellPrice)}코인에 매도합니다. 되돌릴 수 없어요.")
            },
            confirmButton = {
                TextButton(onClick = { onSellOwned(target.id); pendingSell = null }) {
                    Text("매도", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSell = null }) { Text("취소") }
            },
            containerColor = SpaceNavy,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = GoldAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatChipMini(text: String, color: Color, @DrawableRes iconRes: Int? = null) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            if (iconRes != null) {
                Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(12.dp))
            }
            Text(text, color = color, style = MaterialTheme.typography.labelSmall)
        }
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
