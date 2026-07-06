package com.doge.simulator.presentation.screen.planet

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.domain.model.PlanetMetaDataTable
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.effectiveProduction
import com.doge.simulator.presentation.component.PlanetLevelBadge
import com.doge.simulator.presentation.component.rememberLiveCoinDisplay
import com.doge.simulator.presentation.viewmodel.PlanetViewModel
import com.doge.simulator.ui.theme.*

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
    var showSellDialog by remember { mutableStateOf(false) }

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
        val (upgradeCoinCost, upgradeResourceCost) = GameConstants.planetUpgradeCost(planet.level)
        val coins by viewModel.coins.collectAsState()
        val canUpgrade = planet.level < GameConstants.PLANET_MAX_LEVEL &&
                coins >= upgradeCoinCost &&
                upgradeResourceCost.all { (type, amount) ->
                    (resources.firstOrNull { it.type == type }?.amount ?: 0L) >= amount
                }
        val successRate = GameConstants.UPGRADE_SUCCESS_RATES[planet.level] ?: 0f
        val isDangerZone = planet.level >= GameConstants.DANGER_ZONE_START

        val liveProfit = rememberLiveCoinDisplay(
            baseCoins = planet.totalProfit,
            netPerMin = planet.effectiveProduction
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val planetCode = planet.variantId.substringAfterLast("-")
                Text(
                    text = "🪐 ${meta?.displayName ?: planet.type.name}  #$planetCode",
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
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )
            }

            // 스탯 카드
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("스탯", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow("생산량", "${planet.effectiveProduction}/분", StatusGreen)
                    DetailRow("시간 수익", "+${"%,d".format(planet.effectiveProduction * 60L)} 코인/시", GoldAccent)
                    DetailRow("위험도", "${planet.risk}", StatusYellow)
                    DetailRow("이벤트율", "${planet.eventRate}%", SpaceAccent)
                    DetailRow("희귀도", meta?.rarity?.name ?: "", GoldAccent)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 자원 드롭 카드 — 현재 등급·강화 레벨이 반영된 실제 드롭 확률
            if (meta != null && meta.resourceDrops.isNotEmpty()) {
                val rarityMultiplier = GameConstants.RARITY_RESOURCE_MULTIPLIER[meta.rarity] ?: 1.0
                val levelMultiplier = GameConstants.planetLevelMultiplier(planet.level)
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("분당 드롭 자원", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(10.dp))
                        meta.resourceDrops.entries.forEachIndexed { index, (type, baseChance) ->
                            if (index > 0) Spacer(modifier = Modifier.height(6.dp))
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
                                    Spacer(modifier = Modifier.width(6.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 투자 현황 카드
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = SpaceNavy),
                border = androidx.compose.foundation.BorderStroke(1.dp, SpaceBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("투자 현황", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow("매입가", "%,d 코인".format(planet.buyPrice), TextPrimary)
                    DetailRow("강화 투자액", "+%,d 코인".format(planet.upgradeInvestment), SpaceAccent)
                    DetailRow("매도 예상가", "%,d 코인".format((baseValue * (1f - GameConstants.SELL_FEE_RATE)).toLong()), GoldAccent)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "누적 수익", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(text = "+%,d 코인".format(liveProfit), color = StatusGreen, style = NumericSmall)
                    }
                }
            }

            // ── 강화 메시지 ─────────────────────────────────────────
            upgradeMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(4.dp),
                    color = when {
                        msg.contains("성공") -> StatusGreen.copy(0.15f)
                        msg.contains("실패") -> StatusRed.copy(0.15f)
                        else -> SpaceMid.copy(0.5f)
                    },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(msg, color = when {
                        msg.contains("성공") -> StatusGreen
                        msg.contains("실패") -> StatusRed
                        else -> TextSecondary
                    }, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 행성 강화 카드 ───────────────────────────────────
            if (planet.level < GameConstants.PLANET_MAX_LEVEL) {
                Card(shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = SpaceNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp,
                        if (isDangerZone) StatusYellow.copy(0.4f) else SpaceBlue),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("행성 강화", color = if (isDangerZone) StatusYellow else GoldAccent,
                                    style = MaterialTheme.typography.labelMedium)
                                Text("Lv.${planet.level} → Lv.${planet.level + 1}",
                                    color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                                Text("성공률 ${(successRate * 100).toInt()}% · ${if (isDangerZone) "⚠ 실패 시 레벨 하락" else "실패 시 레벨 유지"}",
                                    color = if (isDangerZone) StatusYellow else TextSecondary,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                color = if (isDangerZone) StatusYellow.copy(0.15f) else SpaceBlue.copy(0.3f)) {
                                Text(if (isDangerZone) "⚠ 위험구간" else "안전구간",
                                    color = if (isDangerZone) StatusYellow else SpaceAccent,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                Text("비용", color = TextDisabled, style = MaterialTheme.typography.labelSmall)
                                Text("%,d코인".format(upgradeCoinCost),
                                    color = if (coins >= upgradeCoinCost) GoldAccent else StatusRed,
                                    style = NumericXSmall)
                                if (upgradeResourceCost.isNotEmpty()) {
                                    Text(upgradeResourceCost.entries.joinToString(" · ") { "${it.key.displayName}×${it.value}" },
                                        color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.upgradePlanet(planet) },
                                enabled = canUpgrade,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDangerZone) StatusYellow else GoldAccent,
                                    contentColor = SpaceDark),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                            ) { Text("강화 시도", style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp),
                    color = GoldAccent.copy(0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(0.4f))) {
                    Text("🏆 최대 레벨 달성!", color = GoldAccent,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showSellDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRed,
                    contentColor = TextPrimary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
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
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text(text = value, color = color, style = NumericSmall)
    }
}
