package com.doge.simulator.presentation.screen.planet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.effectiveProduction
import com.doge.simulator.presentation.component.PlanetLevelBadge
import com.doge.simulator.presentation.component.rememberLiveCoinDisplay
import com.doge.simulator.presentation.viewmodel.PlanetViewModel
import com.doge.simulator.presentation.viewmodel.UndoableUpgradeFailure
import com.doge.simulator.presentation.viewmodel.UpgradeMessage
import com.doge.simulator.presentation.viewmodel.UpgradeMessageTone
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanetDetailScreen(
    planetId: String,
    onBack: () -> Unit,
    viewModel: PlanetViewModel = hiltViewModel()
) {
    val planets by viewModel.planets.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val planet = planets.firstOrNull { it.id == planetId }
    val upgradeMessage by viewModel.upgradeMessage.collectAsState()
    val undoableFailure by viewModel.undoableFailure.collectAsState()
    val activity = LocalContext.current.findActivity()
    var showSellDialog by remember { mutableStateOf(false) }
    var showUpgradeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = planet?.let {
                            val code = it.variantId.substringAfterLast("-")
                            val name = PlanetMetaDataTable.data[it.type]?.displayName ?: it.type.name
                            "$name  #$code"
                        } ?: "행성 상세",
                        color = GoldAccent,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceNavy)
            )
        },
        containerColor = SpaceDark
    ) { padding ->
        if (planet == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("행성을 찾을 수 없습니다.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }

        val meta = PlanetMetaDataTable.data[planet.type]
        val baseValue = planet.buyPrice + planet.upgradeInvestment
        val profitRate = ((baseValue - planet.buyPrice).toFloat() / planet.buyPrice * 100).toInt()
        val coins by viewModel.coins.collectAsState()

        val liveProfit = rememberLiveCoinDisplay(
            baseCoins = planet.totalProfit,
            netPerMin = planet.effectiveProduction
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
        ) {
            val imageUrl = meta?.variants?.firstOrNull { it.variantId == planet.variantId }?.imageUrl
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .memoryCacheKey(imageUrl)
                        .diskCacheKey(imageUrl)
                        .build(),
                    contentDescription = meta?.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SpaceMid),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                val planetCode = planet.variantId.substringAfterLast("-")
                Image(
                    painter = painterResource(R.drawable.ic_ui_planet),
                    contentDescription = null,
                    modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                )
                Text(
                    text = "${meta?.displayName ?: planet.type.name}  #$planetCode",
                    color = GoldAccent,
                    style = MaterialTheme.typography.titleMedium
                )
                PlanetLevelBadge(level = planet.level)
            }
            meta?.let {
                Text(
                    text = it.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.lg)
                )
            }

            // 스탯 카드
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .textured(shape = RoundedCornerShape(6.dp), baseColor = SpaceNavy)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text("스탯", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(Spacing.md))
                    DetailRow("생산량", "${planet.effectiveProduction}/분", StatusGreen)
                    DetailRow("시간 수익", "+${"%,d".format(planet.effectiveProduction * 60L)} 코인/시", GoldAccent)
                    DetailRow("위험도", "${planet.risk}", StatusYellow)
                    DetailRow("이벤트율", "${planet.eventRate}%", SpaceAccent)
                    DetailRow("희귀도", meta?.rarity?.name ?: "", GoldAccent)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // 자원 드롭 카드 — 현재 등급·강화 레벨이 반영된 실제 드롭 확률
            if (meta != null && meta.resourceDrops.isNotEmpty()) {
                val rarityMultiplier = GameConstants.RARITY_RESOURCE_MULTIPLIER[meta.rarity] ?: 1.0
                val levelMultiplier = GameConstants.planetLevelMultiplier(planet.level)
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .textured(shape = RoundedCornerShape(6.dp), baseColor = SpaceNavy)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Text("분당 드롭 자원", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        meta.resourceDrops.entries.forEachIndexed { index, (type, baseChance) ->
                            if (index > 0) Spacer(modifier = Modifier.height(Spacing.sm))
                            val effectiveChance = baseChance * rarityMultiplier * levelMultiplier
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(type.iconRes),
                                        contentDescription = type.displayName,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text(type.displayName, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "%.1f%%/분".format(effectiveChance),
                                    color = SpaceAccent,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // 투자 현황 카드
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .textured(shape = RoundedCornerShape(6.dp), baseColor = SpaceNavy)
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text("투자 현황", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(Spacing.md))
                    DetailRow("매입가", "%,d 코인".format(planet.buyPrice), TextPrimary)
                    DetailRow("강화 투자액", "+%,d 코인".format(planet.upgradeInvestment), SpaceAccent)
                    DetailRow("매도 예상가", "%,d 코인".format((baseValue * (1f - GameConstants.SELL_FEE_RATE)).toLong()), GoldAccent)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "누적 수익", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(text = "+%,d 코인".format(liveProfit), color = StatusGreen, style = NumericSmall)
                    }
                }
            }

            // ── 강화 진입 ───────────────────────────────────────
            if (planet.level < GameConstants.PLANET_MAX_LEVEL) {
                Button(
                    onClick = { showUpgradeSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
                    border = ButtonDepth.highlightBorder,
                    elevation = ButtonDepth.elevation(),
                    contentPadding = ButtonPadding.fullWidthCta
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("강화하기", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(
                            "Lv.${planet.level} → Lv.${planet.level + 1}",
                            style = NumericXSmall,
                            color = SpaceDark.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp),
                    color = GoldAccent.copy(0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(0.4f))) {
                    Row(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_trophy),
                            contentDescription = null,
                            modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                        )
                        Text("최대 레벨 달성!", color = GoldAccent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Button(
                onClick = { showSellDialog = true },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRed,
                    contentColor = TextPrimary
                ),
                border = ButtonDepth.highlightBorder,
                elevation = ButtonDepth.elevation(),
                contentPadding = ButtonPadding.fullWidthCta
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "매도하기", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "%,d 코인 수령 예정".format((baseValue * (1f - GameConstants.SELL_FEE_RATE)).toLong()),
                        style = NumericXSmall,
                        color = TextPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showSellDialog) {
            SellConfirmDialog(
                planet = planet,
                onConfirm = {
                    viewModel.sellPlanet(planet)
                    showSellDialog = false
                    onBack()
                },
                onDismiss = { showSellDialog = false }
            )
        }

        if (showUpgradeSheet) {
            PlanetUpgradeDialog(
                planet = planet,
                coins = coins,
                resources = resources,
                upgradeMessage = upgradeMessage,
                undoableFailure = undoableFailure,
                onUpgrade = { viewModel.upgradePlanet(planet) },
                onUndo = { viewModel.undoFailedUpgrade(activity) },
                onDismiss = { showUpgradeSheet = false }
            )
        }
    }
}

@Composable
private fun PlanetUpgradeDialog(
    planet: Planet,
    coins: Long,
    resources: List<Resource>,
    upgradeMessage: UpgradeMessage?,
    undoableFailure: UndoableUpgradeFailure?,
    onUpgrade: () -> Unit,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
) {
    val isMaxLevel = planet.level >= GameConstants.PLANET_MAX_LEVEL
    val (upgradeCoinCost, upgradeResourceCost) = GameConstants.planetUpgradeCost(planet.level)
    val canUpgrade = !isMaxLevel &&
            coins >= upgradeCoinCost &&
            upgradeResourceCost.all { (type, amount) ->
                (resources.firstOrNull { it.type == type }?.amount ?: 0L) >= amount
            }
    val successRate = GameConstants.UPGRADE_SUCCESS_RATES[planet.level] ?: 0f
    val isDangerZone = planet.level >= GameConstants.DANGER_ZONE_START

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceNavy,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(text = "행성 강화", color = if (isDangerZone) StatusYellow else GoldAccent,
                style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                if (isMaxLevel) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp),
                        color = GoldAccent.copy(0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(0.4f))) {
                        Row(
                            modifier = Modifier.padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_ui_trophy),
                                contentDescription = null,
                                modifier = Modifier.size(IconGlyphSize.medium.value.dp)
                            )
                            Text("최대 레벨 달성!", color = GoldAccent, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Lv.${planet.level} → Lv.${planet.level + 1}",
                            color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = if (isDangerZone) StatusYellow.copy(0.15f) else SpaceBlue.copy(0.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
                                if (isDangerZone) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_ui_danger),
                                        contentDescription = null,
                                        modifier = Modifier.size(IconGlyphSize.small.value.dp)
                                    )
                                }
                                Text(if (isDangerZone) "위험구간" else "안전구간",
                                    color = if (isDangerZone) StatusYellow else SpaceAccent,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text("성공률 ${(successRate * 100).toInt()}% · ",
                            color = if (isDangerZone) StatusYellow else TextSecondary,
                            style = MaterialTheme.typography.labelSmall)
                        if (isDangerZone) {
                            Image(
                                painter = painterResource(R.drawable.ic_ui_danger),
                                contentDescription = null,
                                modifier = Modifier.size(IconGlyphSize.small.value.dp)
                            )
                        }
                        Text(if (isDangerZone) "실패 시 레벨 하락" else "실패 시 레벨 유지",
                            color = if (isDangerZone) StatusYellow else TextSecondary,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                    DetailRow("비용", "%,d 코인".format(upgradeCoinCost),
                        if (coins >= upgradeCoinCost) GoldAccent else StatusRed)
                    if (upgradeResourceCost.isNotEmpty()) {
                        Text(upgradeResourceCost.entries.joinToString(" · ") { "${it.key.displayName}×${it.value}" },
                            color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // ── 강화 메시지 ─────────────────────────────────────
                upgradeMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(Spacing.md))
                    val tint = when (msg.tone) {
                        UpgradeMessageTone.SUCCESS -> StatusGreen
                        UpgradeMessageTone.FAIL -> StatusRed
                        UpgradeMessageTone.INFO -> TextSecondary
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tint.copy(alpha = if (msg.tone == UpgradeMessageTone.INFO) 0.5f else 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            if (msg.iconRes != null) {
                                Image(
                                    painter = painterResource(msg.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(msg.text, color = tint, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (undoableFailure != null && undoableFailure.planetId == planet.id) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = onUndo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceAccent)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Image(
                                painter = painterResource(R.drawable.ic_ui_ad),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("광고 보고 되돌리기", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isMaxLevel) {
                TextButton(onClick = onDismiss) {
                    Text("확인", color = GoldAccent, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                TextButton(onClick = onUpgrade, enabled = canUpgrade) {
                    Text("강화 시도",
                        color = if (canUpgrade) (if (isDangerZone) StatusYellow else GoldAccent) else TextSecondary,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        dismissButton = {
            if (!isMaxLevel) {
                TextButton(onClick = onDismiss) {
                    Text("닫기", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(text = value, color = color, style = NumericSmall)
    }
}
