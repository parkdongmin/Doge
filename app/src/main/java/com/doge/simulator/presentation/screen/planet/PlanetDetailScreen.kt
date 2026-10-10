package com.doge.simulator.presentation.screen.planet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.doge.simulator.domain.model.marketValue
import com.doge.simulator.domain.model.normalProductionAtLevel
import com.doge.simulator.domain.model.isBroken
import com.doge.simulator.domain.model.maintenancePlan
import com.doge.simulator.domain.model.maintenanceResourceCost
import com.doge.simulator.domain.model.upgradeCost
import com.doge.simulator.domain.model.marketChange
import com.doge.simulator.domain.model.normalProduction
import com.doge.simulator.presentation.component.HelpChip
import com.doge.simulator.presentation.component.PlanetLevelBadge
import com.doge.simulator.presentation.component.rememberLiveCoinDisplay
import com.doge.simulator.presentation.viewmodel.PlanetViewModel
import com.doge.simulator.presentation.viewmodel.UndoableUpgradeFailure
import com.doge.simulator.presentation.viewmodel.UpgradeMessage
import com.doge.simulator.presentation.viewmodel.UpgradeMessageTone
import com.doge.simulator.presentation.viewmodel.UpgradePhase
import com.doge.simulator.ui.theme.*
import kotlin.math.roundToInt
import com.doge.simulator.presentation.component.GameButton
import com.doge.simulator.presentation.component.GameButtonSize
import com.doge.simulator.presentation.component.GameButtonStyle
import com.doge.simulator.presentation.component.GameDialog
import com.doge.simulator.presentation.component.InfoDialog
import com.doge.simulator.presentation.component.GameDialogButtons
import com.doge.simulator.util.UpgradeHaptic
import com.doge.simulator.util.findActivity
import com.doge.simulator.util.vibrateUpgradeResult
import com.doge.simulator.presentation.component.PixelIcons
import com.doge.simulator.presentation.component.DogeTopBar
import com.doge.simulator.presentation.component.NightSkyBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanetDetailScreen(
    planetId: String,
    onBack: () -> Unit,
    viewModel: PlanetViewModel = hiltViewModel()
) {
    val planets by viewModel.planets.collectAsState()
    val resources by viewModel.resources.collectAsState()
    // 강화 결과 공개 전까지는 강화 전 모습으로 고정(레벨 배지 등이 연출 도중 먼저 바뀌는 스포 방지)
    val upgradeDisplayFreeze by viewModel.upgradeDisplayFreeze.collectAsState()
    val planet = upgradeDisplayFreeze?.takeIf { it.id == planetId }
        ?: planets.firstOrNull { it.id == planetId }
    val upgradeMessage by viewModel.upgradeMessage.collectAsState()
    val upgradePhase by viewModel.upgradePhase.collectAsState()
    val undoableFailure by viewModel.undoableFailure.collectAsState()
    val activity = LocalContext.current.findActivity()
    var showSellDialog by remember { mutableStateOf(false) }
    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showStatsInfo by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    val maintenanceMessage by viewModel.maintenanceMessage.collectAsState()

    // 하위 화면 공통 밤하늘 배경(NightSkyBackground) 위에 투명 Scaffold.
    NightSkyBackground(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                DogeTopBar(
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
                    onBack = onBack
                )
            },
            containerColor = Color.Transparent
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
            val investedAmount = planet.buyPrice + planet.upgradeInvestment
            val baseValue = planet.marketValue // 0 이상 (악재가 겹쳐도 시세는 0에서 바닥)
            val estimatedProceeds = baseValue - (baseValue * GameConstants.SELL_FEE_RATE).toLong()
            // 표시용 시세 변동 — 매입가+강화액을 다 깎는 "전액 손실"(-100%)까지만
            val displayAdjustment = planet.marketChange
            val adjustmentPct = if (investedAmount > 0L) (displayAdjustment * 100 / investedAmount).toInt() else 0 // 대략치
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("스탯", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            HelpChip(onClick = { showStatsInfo = true }, contentDescription = "스탯 설명 보기")
                        }
                        Spacer(modifier = Modifier.height(Spacing.md))
                        val hourlyEarnings = planet.effectiveProduction * 60L
                        DetailRow(
                            "생산량",
                            "${planet.effectiveProduction}/분",
                            if (planet.effectiveProduction >= 0) StatusGreen else StatusRed
                        )
                        DetailRow(
                            label = if (planet.isBroken) "고장 중" else "생산 진행",
                            value = "${if (hourlyEarnings >= 0) "+" else ""}${"%,d".format(hourlyEarnings)} 코인/시",
                            color = if (hourlyEarnings >= 0) GoldAccent else StatusRed
                        )
                        if (displayAdjustment != 0L) {
                            val marketColor = if (displayAdjustment > 0L) StatusGreen else StatusRed
                            val sign = if (displayAdjustment > 0L) "+" else ""
                            val pctSign = if (adjustmentPct > 0) "+" else ""
                            DetailRow(
                                "시세 변동",
                                "$sign${"%,d".format(displayAdjustment)} 코인 (${pctSign}$adjustmentPct%)",
                                marketColor
                            )
                        }
                        val eventIntervalHours = GameConstants.planetEventIntervalHours(planet.risk)
                        val intervalText = if (eventIntervalHours < 1.0) {
                            "약 ${(eventIntervalHours * 60).toInt()}분마다"
                        } else {
                            "약 ${eventIntervalHours.toInt()}시간마다"
                        }
                        DetailRow("이벤트 간격", intervalText, StatusYellow)
                        DetailRow("악재 확률", "${planet.eventRate}%", SpaceAccent)
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
                                        style = NumericSmall
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
                        DetailRow("투자액", "%,d 코인".format(investedAmount), TextPrimary)
                        DetailRow(
                            "현재 매도가",
                            "%,d 코인".format(estimatedProceeds),
                            when {
                                planet.marketValue > investedAmount -> StatusGreen
                                planet.marketValue < investedAmount -> StatusRed
                                else -> GoldAccent
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "누적 수익", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            val profitSign = if (liveProfit > 0) "+" else ""
                            val profitColor = if (liveProfit >= 0) StatusGreen else StatusRed
                            Text(text = "$profitSign${"%,d".format(liveProfit)} 코인", color = profitColor, style = NumericSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                // ── 강화·매도 액션 바 ─────────────────────────────────
                // 페이지의 다른 모든 블록이 풀폭이라, 매도 버튼만 따로 작게 가운데 두면 그것대로
                // 붕 떠 보인다. 대신 강화(주 액션)와 한 줄에 나란히 두고 비율로 위계를 준다.
                // 고장 중엔 강화 자리를 정비가 대신한다(최대 레벨이어도 정비는 필요)
                if (planet.isBroken) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        GameButton(
                            text = "매도",
                            onClick = { showSellDialog = true },
                            modifier = Modifier.weight(0.32f).fillMaxHeight(),
                            style = GameButtonStyle.Danger,
                            size = GameButtonSize.Large
                        )
                        GameButton(
                            text = "정비",
                            onClick = { showMaintenanceDialog = true },
                            modifier = Modifier.weight(0.68f).fillMaxHeight(),
                            style = GameButtonStyle.Gold,
                            size = GameButtonSize.Large,
                            subText = "%,d 코인".format(planet.maintenancePlan { type -> resources.ownedAmount(type) }.totalCoins)
                        )
                    }
                } else if (planet.level < GameConstants.PLANET_MAX_LEVEL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // 보조 액션은 왼쪽, 주 액션은 오른쪽 — 강화 다이얼로그의 닫기/강화 시도
                        // 배치와 같은 규칙(안드로이드/머티리얼 관례)
                        GameButton(
                            text = "매도",
                            onClick = { showSellDialog = true },
                            modifier = Modifier.weight(0.32f).fillMaxHeight(),
                            style = GameButtonStyle.Danger,
                            size = GameButtonSize.Large
                        )
                        GameButton(
                            text = "강화",
                            onClick = { showUpgradeSheet = true },
                            modifier = Modifier.weight(0.68f).fillMaxHeight(),
                            style = GameButtonStyle.Gold,
                            size = GameButtonSize.Large,
                            subText = "Lv.${planet.level} → Lv.${planet.level + 1}"
                        )
                    }
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
                    GameButton(
                        text = "매도",
                        onClick = { showSellDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        style = GameButtonStyle.Danger,
                        size = GameButtonSize.Large
                    )
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
                    upgradePhase = upgradePhase,
                    undoableFailure = undoableFailure,
                    onUpgrade = { viewModel.upgradePlanet(planet) },
                    onUndo = { viewModel.undoFailedUpgrade(activity) },
                    onAcknowledgeResult = { viewModel.dismissUpgradeResult() },
                    onDismiss = {
                        // 결과를 확인 안 하고 그냥 닫아도, 다음에 다시 열었을 때 지난 결과가
                        // 남아있지 않도록 초기화한다
                        viewModel.dismissUpgradeResult()
                        showUpgradeSheet = false
                    }
                )
            }

            if (showStatsInfo) {
                StatsInfoDialog(onDismiss = { showStatsInfo = false })
            }

            // 고장이 아니게 되면(정비 성공·다른 경로) 자동으로 닫힌다
            if (showMaintenanceDialog && planet.isBroken) {
                MaintenanceDialog(
                    planet = planet,
                    coins = coins,
                    resources = resources,
                    message = maintenanceMessage,
                    onMaintain = { viewModel.maintainPlanet(planet) { showMaintenanceDialog = false } },
                    onDismiss = {
                        viewModel.clearMaintenanceMessage()
                        showMaintenanceDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PlanetUpgradeDialog(
    planet: Planet,
    coins: Long,
    resources: List<Resource>,
    upgradeMessage: UpgradeMessage?,
    upgradePhase: UpgradePhase,
    undoableFailure: UndoableUpgradeFailure?,
    onUpgrade: () -> Unit,
    onUndo: () -> Unit,
    onAcknowledgeResult: () -> Unit,
    onDismiss: () -> Unit
) {
    val isMaxLevel = planet.level >= GameConstants.PLANET_MAX_LEVEL
    val (upgradeCoinCost, upgradeResourceCost) = planet.upgradeCost
    val canUpgrade = !isMaxLevel &&
            coins >= upgradeCoinCost &&
            upgradeResourceCost.all { (type, amount) -> resources.ownedAmount(type) >= amount }
    val successRate = GameConstants.UPGRADE_SUCCESS_RATES[planet.level] ?: 0f
    val isDangerZone = planet.level >= GameConstants.DANGER_ZONE_START
    val dropChance = GameConstants.UPGRADE_DROP_CHANCES[planet.level] ?: 0f
    // 결과가 나오는 중(Charging)에는 실수로 중복 시도하거나 다이얼로그를 닫지 못하게 막는다
    val isResolving = upgradePhase is UpgradePhase.Charging
    // 결과 공개 중엔 다른 카드들보다 테두리를 강조해 "지금 중요한 순간"이라는 걸 프레임 전체로도 알려준다
    val borderColor = when {
        upgradePhase is UpgradePhase.Revealing && upgradePhase.isDangerFail -> StatusRed
        upgradePhase is UpgradePhase.Revealing && upgradePhase.message.tone == UpgradeMessageTone.SUCCESS -> StatusGreen
        isDangerZone -> StatusYellow
        else -> SpaceBlue
    }

    GameDialog(
        title = "행성 강화",
        onDismissRequest = { if (!isResolving) onDismiss() },
        dismissOnClickOutside = !isResolving,
        borderColor = borderColor,
        buttons = {
            when {
                isMaxLevel -> GameDialogButtons(confirmText = "확인", onConfirm = onDismiss)
                upgradePhase is UpgradePhase.Revealing -> GameDialogButtons(
                    confirmText = "확인",
                    onConfirm = onAcknowledgeResult,
                    dismissText = "닫기",
                    onDismiss = onDismiss
                )
                else -> GameDialogButtons(
                    confirmText = if (isResolving) "강화 중..." else "강화",
                    onConfirm = onUpgrade,
                    confirmStyle = GameButtonStyle.Gold,
                    confirmEnabled = canUpgrade && upgradePhase == UpgradePhase.Idle,
                    dismissText = "닫기",
                    onDismiss = onDismiss,
                    dismissEnabled = !isResolving
                )
            }
        }
    ) {
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
            Spacer(modifier = Modifier.height(Spacing.md))
            UpgradeOutcomeCells(successRate = successRate, dropChance = if (isDangerZone) dropChance else 0f)

            Spacer(modifier = Modifier.height(Spacing.lg))
            CostTable(
                coinCost = upgradeCoinCost,
                coins = coins,
                resourceCost = upgradeResourceCost.mapValues { it.value.toLong() },
                resources = resources
            )

            // ── 강화 성공 시 효과 미리보기 ───────────────────────
            // 위 스탯 카드의 "생산 진행"과 같은 시간당 단위 — 분당(+1)으론 이득이 너무 작아 보였다
            Spacer(modifier = Modifier.height(Spacing.md))
            val curHourly = planet.effectiveProduction * 60L
            val nextHourly = planet.normalProductionAtLevel(planet.level + 1).toLong() * 60L
            DetailRow(
                "생산량",
                "%,d → %,d 코인/시".format(curHourly, nextHourly),
                if (nextHourly > curHourly) StatusGreen else TextSecondary
            )
        }

        // ── 강화 진행 상태(충전 중 / 결과 공개) ───────────────
        when (upgradePhase) {
            is UpgradePhase.Charging -> {
                Spacer(modifier = Modifier.height(Spacing.md))
                ChargingIndicator(isDangerZone = upgradePhase.isDangerZone)
            }
            is UpgradePhase.Revealing -> {
                Spacer(modifier = Modifier.height(Spacing.md))
                UpgradeRevealCard(message = upgradePhase.message, isDangerFail = upgradePhase.isDangerFail)
            }
            UpgradePhase.Idle -> {
                // 검증 실패(코인/자원 부족·최대 레벨) 등 즉시 표시되는 단순 메시지
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
            }
        }

        if (undoableFailure != null && undoableFailure.planetId == planet.id) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            GameButton(
                text = "광고 보고 되돌리기",
                onClick = onUndo,
                modifier = Modifier.fillMaxWidth(),
                style = GameButtonStyle.Primary,
                leadingIcon = R.drawable.ic_ui_ad
            )
        }

    }
}

// "스탯" 카드의 ⓘ 아이콘으로 여는 용어 설명 팝업. 위험도/이벤트율 같은 원래 숫자만으로는
// 처음 보는 플레이어가 뜻을 알 수 없어서, 화면엔 사람이 읽을 수 있는 값(간격 시간/확률)으로
// 보여주고 각 항목이 정확히 뭘 뜻하는지는 여기서 한 번에 설명한다
@Composable
private fun StatsInfoDialog(onDismiss: () -> Unit) {
    InfoDialog(title = "스탯 설명", onDismiss = onDismiss) {
        StatsInfoEntry("생산량", "이 행성이 1분에 만드는 코인이에요. 레벨과 이벤트 효과가 반영돼요.")
        StatsInfoEntry("생산 진행 / 고장 중", "생산량을 1시간 기준으로 보여줘요. 큰 악재가 뜨면 고장 나서 마이너스가 되고, 정비할 때까지 코인이 줄어요.")
        StatsInfoEntry("강화", "레벨이 오르면 생산량과 자원 드롭량이 늘어요. 강화에 쓴 코인은 매도가에 더해지지만 자원은 돌려받지 못해요.")
        StatsInfoEntry("정비", "고장 난 행성을 원래 생산량으로 되돌려요. 코인과 자원이 들고, 고장 중엔 강화할 수 없고 매도가도 떨어져요.")
        StatsInfoEntry("시세 변동", "악재·호재·고장으로 달라진 매도가예요. 괄호 안 %는 투자액 대비 비율이에요.")
        StatsInfoEntry("이벤트 간격", "이 행성에 이벤트가 얼마나 자주 오는지예요. 위험한 타입일수록 자주 와요.")
        StatsInfoEntry("악재 확률", "이벤트가 나쁜 쪽으로 나올 확률이에요. 희귀도가 높을수록 낮아요.", isLast = true)
    }
}

// 고장 행성 정비 — 지금(마이너스) → 정비 후(플러스) 생산량·매도가와 비용을 보여주고 확인받는다
@Composable
private fun MaintenanceDialog(
    planet: Planet,
    coins: Long,
    resources: List<Resource>,
    message: String?,
    onMaintain: () -> Unit,
    onDismiss: () -> Unit
) {
    val plan = planet.maintenancePlan { type -> resources.ownedAmount(type) }
    val cost = plan.totalCoins
    val fixed = planet.copy(lossMultiplier = 0.0)
    GameDialog(
        title = "행성 정비",
        onDismissRequest = onDismiss,
        borderColor = StatusRed,
        buttons = {
            GameDialogButtons(
                confirmText = "정비",
                onConfirm = onMaintain,
                confirmStyle = GameButtonStyle.Gold,
                confirmEnabled = coins >= cost,
                dismissText = "닫기",
                onDismiss = onDismiss
            )
        }
    ) {
        // 위 스탯 카드("고장 중 −300 코인/시")와 같은 시간당 단위
        DetailRow(
            "생산량",
            "%,d → +%,d 코인/시".format(planet.effectiveProduction * 60L, planet.normalProduction.toLong() * 60L),
            StatusGreen
        )
        DetailRow(
            "매도가",
            "%,d → %,d 코인".format(planet.marketValue, fixed.marketValue),
            StatusGreen
        )

        Spacer(modifier = Modifier.height(Spacing.md))
        // 코인 필요량은 자원 대체분까지 더한 실제 지불액. 자원은 모자라도 코인으로 대신 낼 수 있어
        // "불가"를 뜻하는 빨강 대신 노랑
        CostTable(
            coinCost = cost,
            coins = coins,
            resourceCost = planet.maintenanceResourceCost,
            resources = resources,
            resourceShortColor = StatusYellow
        )
        if (plan.substituteCoins > 0) {
            Text(
                "모자란 자원 대신 %,d 코인을 더 내요".format(plan.substituteCoins),
                color = StatusYellow,
                style = BodyReading,
                modifier = Modifier.padding(top = Spacing.xxs)
            )
        }
        message?.let {
            Text(it, color = StatusRed, style = BodyReading, modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

// 강화 한 번의 결과를 칸으로 — "실패 시 N% 확률로 하락"은 조건부 확률이라 한 번 더 계산해야 했다.
// 하락이 없는 안전 구간은 성공/유지 두 칸. 반올림 오차는 유지 칸이 흡수해 합이 항상 100%
@Composable
private fun UpgradeOutcomeCells(successRate: Float, dropChance: Float) {
    val successPct = (successRate * 100).roundToInt()
    val dropPct = ((1f - successRate) * dropChance * 100).roundToInt()
    val keepPct = 100 - successPct - dropPct
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutcomeCell("성공", successPct, StatusGreen, Modifier.weight(1f))
        OutcomeCell("유지", keepPct, TextSecondary, Modifier.weight(1f))
        if (dropChance > 0f) OutcomeCell("하락", dropPct, StatusRed, Modifier.weight(1f))
    }
}

@Composable
private fun OutcomeCell(label: String, percent: Int, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = color, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text("$percent%", color = color, style = NumericMedium)
        }
    }
}

// 강화·정비 비용 — 코인과 자원을 같은 형식(아이콘·이름·보유/필요)으로 한 줄씩. 모자란 줄만 색으로 표시.
// 코인은 대체 수단이 없어 항상 빨강, 자원은 정비처럼 코인으로 대신 낼 수 있으면 노랑을 넘긴다
@Composable
private fun CostTable(
    coinCost: Long,
    coins: Long,
    resourceCost: Map<ResourceType, Long>,
    resources: List<Resource>,
    resourceShortColor: Color = StatusRed
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("비용", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("보유 / 필요", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
    Spacer(modifier = Modifier.height(Spacing.xxs))
    CostRow(R.drawable.ic_ui_coin, "코인", coins, coinCost, StatusRed)
    resourceCost.forEach { (type, amount) ->
        CostRow(type.iconRes, type.displayName, resources.ownedAmount(type), amount, resourceShortColor)
    }
}

@Composable
private fun CostRow(@DrawableRes iconRes: Int, name: String, owned: Long, needed: Long, shortColor: Color) {
    val enough = owned >= needed
    val color = if (enough) TextPrimary else shortColor
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(IconGlyphSize.medium.value.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(name, color = color, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text("%,d / %,d".format(owned, needed), color = color, style = NumericSmall)
    }
}

private fun List<Resource>.ownedAmount(type: ResourceType): Long =
    firstOrNull { it.type == type }?.amount ?: 0L

@Composable
private fun StatsInfoEntry(term: String, description: String, isLast: Boolean = false) {
    Column {
        Text(term, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(description, color = TextSecondary, style = BodyReading)
    }
    if (!isLast) Spacer(modifier = Modifier.height(Spacing.md))
}

// 강화 굴림이 진행되는 동안(결과가 이미 나왔더라도 화면엔 아직 안 보여주는 구간) 긴장감을
// 주는 충전 연출. 위험구간이면 더 빠르고 붉게 펄스쳐 "이번엔 위험하다"는 신호를 미리 준다
@Composable
private fun ChargingIndicator(isDangerZone: Boolean) {
    val transition = rememberInfiniteTransition(label = "upgrade_charge")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isDangerZone) 260 else 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chargePulse"
    )
    val color = if (isDangerZone) StatusRed else GoldAccent

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.08f + 0.10f * pulse),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = pulse)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.lg),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(if (isDangerZone) R.drawable.ic_ui_danger else R.drawable.ic_ui_levelup),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = pulse }
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = if (isDangerZone) "강화 중… 위험합니다!" else "강화 중…",
                color = color,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

// 강화 결과 공개 — 성공/실패/위험구간 하락을 각각 다른 강도로 연출하고, 공개되는 순간
// 결과별로 다른 진동 패턴을 재생해 사운드가 없는 지금 상태에서 임팩트를 대신 전달한다
@Composable
private fun UpgradeRevealCard(message: UpgradeMessage, isDangerFail: Boolean) {
    val context = LocalContext.current
    val scale = remember { Animatable(0.6f) }
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(message) {
        when {
            message.tone == UpgradeMessageTone.SUCCESS -> {
                context.vibrateUpgradeResult(UpgradeHaptic.SUCCESS)
                scale.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
            isDangerFail -> {
                context.vibrateUpgradeResult(UpgradeHaptic.DANGER_FAIL)
                scale.snapTo(1f)
                repeat(3) {
                    shakeX.animateTo(14f, animationSpec = tween(55))
                    shakeX.animateTo(-14f, animationSpec = tween(55))
                }
                shakeX.animateTo(0f, animationSpec = tween(55))
            }
            else -> {
                context.vibrateUpgradeResult(UpgradeHaptic.FAIL)
                scale.snapTo(1f)
                shakeX.animateTo(8f, animationSpec = tween(50))
                shakeX.animateTo(0f, animationSpec = tween(50))
            }
        }
    }

    val tint = when {
        message.tone == UpgradeMessageTone.SUCCESS -> StatusGreen
        message.tone == UpgradeMessageTone.FAIL && isDangerFail -> StatusRed
        message.tone == UpgradeMessageTone.FAIL -> StatusYellow
        else -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = tint.copy(alpha = if (message.tone == UpgradeMessageTone.INFO) 0.5f else 0.18f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                translationX = shakeX.value
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            if (message.iconRes != null) {
                Image(
                    painter = painterResource(message.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(message.text, color = tint, style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
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
