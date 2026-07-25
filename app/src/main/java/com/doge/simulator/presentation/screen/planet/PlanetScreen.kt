package com.doge.simulator.presentation.screen.planet

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaData
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.PlanetType
import com.doge.simulator.domain.model.RarityTier
import com.doge.simulator.domain.model.effectiveProduction
import com.doge.simulator.presentation.component.PlanetLevelBadge
import com.doge.simulator.presentation.viewmodel.PlanetViewModel
import com.doge.simulator.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun PlanetScreen(
    onPlanetClick: (String) -> Unit = {},
    viewModel: PlanetViewModel = hiltViewModel()
) {
    val planets by viewModel.planets.collectAsState()
    val discoveredVariantIds by viewModel.discoveredVariantIds.collectAsState()
    var sellTarget by remember { mutableStateOf<Planet?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("보유 행성", "도감")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
    ) {
        // ── 헤더 ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
        ) {
            Text(
                text = "행성 관리",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "보유 행성을 강화하거나 도감을 통해 목표를 확인하세요",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // ── 탭 ───────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SpaceDark,
            contentColor = SpaceAccent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) SpaceAccent else TextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }
        }

        // ── 탭 콘텐츠 ──────────────────────────────────────────────
        when (selectedTab) {
            0 -> {
                if (planets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(R.drawable.ic_ui_planet),
                                contentDescription = null,
                                modifier = Modifier.size(IconGlyphSize.xlarge.value.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "보유한 행성이 없습니다",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "탐사 화면에서 탐사를 시작해보세요!",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = Spacing.xs)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 8.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        items(planets) { planet ->
                            PlanetListCard(
                                planet = planet,
                                onCardClick = { onPlanetClick(planet.id) },
                                onSellClick = { sellTarget = planet }
                            )
                        }
                    }
                }
            }
            1 -> PlanetCatalogContent(
                discoveredVariantIds = discoveredVariantIds,
                ownedPlanets = planets,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    sellTarget?.let { planet ->
        SellConfirmDialog(
            planet = planet,
            onConfirm = {
                viewModel.sellPlanet(planet)
                sellTarget = null
            },
            onDismiss = { sellTarget = null }
        )
    }
}

@Composable
private fun PlanetListCard(
    planet: Planet,
    onCardClick: () -> Unit,
    onSellClick: () -> Unit
) {
    val meta = PlanetMetaDataTable.data[planet.type]
    val imageUrl = meta?.variants?.firstOrNull { it.variantId == planet.variantId }?.imageUrl

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceMid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCacheKey(imageUrl)
                        .diskCacheKey(imageUrl)
                        .build(),
                    contentDescription = meta?.displayName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SpaceMid),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.None
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(SpaceMid),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_planet),
                        contentDescription = null,
                        modifier = Modifier.size(IconGlyphSize.large.value.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                val planetCode = planet.variantId.substringAfterLast("-")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = meta?.displayName ?: planet.type.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#$planetCode",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                    PlanetLevelBadge(level = planet.level)
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = meta?.description ?: "",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MiniStatChip(iconRes = R.drawable.ic_ui_energy, value = "${planet.effectiveProduction}/분", color = StatusGreen)
                    MiniStatChip(iconRes = R.drawable.ic_ui_coin, value = "+${"%,d".format(planet.effectiveProduction * 60L)}/시", color = GoldAccent)
                }
            }

            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(text = ">", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = Spacing.sm, bottom = Spacing.xs),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onSellClick,
                colors = ButtonDefaults.textButtonColors(contentColor = StatusRed.copy(alpha = 0.7f))
            ) {
                Text("매도", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MiniStatChip(@DrawableRes iconRes: Int, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Image(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(10.dp))
            Text(text = value, color = color, style = NumericXSmall)
        }
    }
}

@Composable
fun SellConfirmDialog(
    planet: Planet,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val meta = PlanetMetaDataTable.data[planet.type]
    val baseValue = planet.buyPrice + planet.upgradeInvestment
    val fee = (baseValue * GameConstants.SELL_FEE_RATE).toLong()
    val netProceeds = baseValue - fee

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceNavy,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "행성 매도", color = GoldAccent, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                val sellDialogCode = planet.variantId.substringAfterLast("-")
                Text(
                    text = "${meta?.displayName ?: planet.type.name}  #$sellDialogCode  (Lv.${planet.level})",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                DialogRow("매입가", "%,d 코인".format(planet.buyPrice), TextPrimary)
                DialogRow("강화 투자액", "+%,d 코인".format(planet.upgradeInvestment), SpaceAccent)
                DialogRow("수수료 (5%)", "-%,d 코인".format(fee), StatusRed)
                HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(vertical = Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(text = "실수령액", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "%,d 코인".format(netProceeds), color = GoldAccent, style = NumericMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("매도 확인", color = StatusRed, style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}

@Composable
private fun DialogRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(text = value, color = valueColor, style = NumericSmall)
    }
}

// ── 행성 도감 ──────────────────────────────────────────────────────────

private val rarityGroups = listOf(
    RarityTier.COMMON,
    RarityTier.UNCOMMON,
    RarityTier.RARE,
    RarityTier.EPIC,
    RarityTier.LEGENDARY
)

private val rarityColor = mapOf(
    RarityTier.COMMON    to Color(0xFF9EA3A8),
    RarityTier.UNCOMMON  to Color(0xFF5DBF7A),
    RarityTier.RARE      to Color(0xFF5B9CF6),
    RarityTier.EPIC      to Color(0xFFB07FE0),
    RarityTier.LEGENDARY to Color(0xFFE8A84C)
)

private val rarityLabel = mapOf(
    RarityTier.COMMON    to "COMMON",
    RarityTier.UNCOMMON  to "UNCOMMON",
    RarityTier.RARE      to "RARE",
    RarityTier.EPIC      to "EPIC",
    RarityTier.LEGENDARY to "LEGENDARY"
)

// variantId → (PlanetType, PlanetMetaData) 역방향 맵 (앱 생명주기 동안 고정)
private val variantLookup: Map<String, Pair<PlanetType, PlanetMetaData>> by lazy {
    PlanetMetaDataTable.data.flatMap { (type, meta) ->
        meta.variants.map { variant -> variant.variantId to (type to meta) }
    }.toMap()
}

@Composable
private fun PlanetCatalogContent(
    discoveredVariantIds: Set<String>,
    ownedPlanets: List<Planet>,
    modifier: Modifier = Modifier
) {
    val ownedVariantIds = remember(ownedPlanets) { ownedPlanets.map { it.variantId }.toSet() }

    // 등급별 전체 variant 수
    val totalByRarity = remember {
        PlanetMetaDataTable.data.values
            .groupBy { it.rarity }
            .mapValues { (_, metas) -> metas.sumOf { it.variants.size } }
    }
    // 발견된 variantId를 등급·타입별로 분류
    val discoveredByRarity = remember(discoveredVariantIds) {
        discoveredVariantIds
            .mapNotNull { id -> variantLookup[id]?.let { (type, meta) -> Triple(id, type, meta) } }
            .groupBy { it.third.rarity }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        rarityGroups.forEach { rarity ->
            val color = rarityColor[rarity] ?: TextSecondary
            val total = totalByRarity[rarity] ?: 0
            val discoveredInRarity = discoveredByRarity[rarity] ?: emptyList()

            // ── 등급 헤더 ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = rarityLabel[rarity] ?: "",
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "발견 ${discoveredInRarity.size} / $total",
                    color = if (discoveredInRarity.size == total && total > 0) color else TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // ── 타입 칩 서브헤더 ───────────────────────────────────
            val typesInRarity = PlanetMetaDataTable.data.values.filter { it.rarity == rarity }
            val discoveredCountByType = discoveredInRarity.groupBy { it.second }.mapValues { it.value.size }
            val totalByType = typesInRarity.associate { it.type to it.variants.size }

            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                typesInRarity.forEach { meta ->
                    val found = discoveredCountByType[meta.type] ?: 0
                    val typeTotal = totalByType[meta.type] ?: 0
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (found > 0) color.copy(alpha = 0.1f) else SpaceMid.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (found > 0) color.copy(alpha = 0.35f) else SpaceMid.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "${meta.displayName}  $found/$typeTotal",
                            color = if (found > 0) color else TextDisabled,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        )
                    }
                }
            }

            // ── 발견 카드 그리드 ───────────────────────────────────
            if (discoveredInRarity.isEmpty()) {
                Text(
                    text = "아직 발견한 행성이 없습니다",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
            } else {
                discoveredInRarity.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        rowItems.forEach { (variantId, _, meta) ->
                            val variantImageUrl = meta.variants
                                .firstOrNull { it.variantId == variantId }?.imageUrl
                            val isOwned = variantId in ownedVariantIds
                            CatalogPlanetCard(
                                variantId = variantId,
                                imageUrl = variantImageUrl,
                                meta = meta,
                                isOwned = isOwned,
                                rarityColor = color,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.xs),
                color = SpaceMid.copy(alpha = 0.3f)
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

@Composable
private fun CatalogPlanetCard(
    variantId: String,
    imageUrl: String?,
    meta: PlanetMetaData,
    isOwned: Boolean,
    rarityColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, rarityColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SpaceMid.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .memoryCacheKey(imageUrl)
                            .build(),
                        contentDescription = meta.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.None
                    )
                }
            }

            Text(
                text = "${meta.displayName} #${variantId.substringAfterLast("-")}",
                color = TextPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Image(
                    painter = painterResource(R.drawable.ic_ui_energy),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${meta.productionMin}–${meta.productionMax} / 분",
                    color = StatusGreen.copy(alpha = 0.8f),
                    style = LabelTiny
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isOwned) rarityColor.copy(alpha = 0.12f) else SpaceMid.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isOwned) "보유 중" else "매도됨",
                    color = if (isOwned) rarityColor else TextDisabled,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                )
            }
        }
    }
}
