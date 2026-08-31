package com.doge.simulator.presentation.screen.hq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.doge.simulator.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.domain.model.Spaceship
import com.doge.simulator.presentation.component.InfoDialog
import com.doge.simulator.presentation.component.ShipInfoContent
import com.doge.simulator.presentation.viewmodel.SpaceshipViewModel
import com.doge.simulator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangarScreen(
    onBack: () -> Unit,
    viewModel: SpaceshipViewModel = hiltViewModel()
) {
    val spaceships by viewModel.spaceships.collectAsState()
    val researchLab by viewModel.researchLab.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val message by viewModel.message.collectAsState()
    var showInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("격납고", color = GoldAccent, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Outlined.Info, "우주선 스탯 설명", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceNavy)
            )
        },
        containerColor = SpaceDark
    ) { padding ->
        if (showInfo) {
            InfoDialog(title = "우주선 스탯", onDismiss = { showInfo = false }) { ShipInfoContent() }
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            message?.let {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    shape = RoundedCornerShape(8.dp), color = SpaceBlue.copy(0.3f)) {
                    Text(it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("보유", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("${spaceships.size}/${researchLab.maxSpaceships}척", color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("코인", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text("%,d".format(coins), color = GoldAccent, style = NumericXSmall)
                }
            }

            HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(horizontal = Spacing.lg))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 구매 버튼
                item {
                    val canBuy = spaceships.size < researchLab.maxSpaceships && coins >= GameConstants.SCOUT_SHIP_BASE_COST
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, GoldAccent.copy(0.4f)),
                        modifier = Modifier.textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.spaceship_2),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.lg))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("정찰선", color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("탑승 ${GameConstants.SCOUT_CREW_BASE}명 · 속도 ${GameConstants.SCOUT_SPEED_BASE} · 적재 ${GameConstants.SCOUT_CARGO_BASE}",
                                    color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.buyShip() },
                                enabled = canBuy,
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = SpaceDark),
                                border = ButtonDepth.highlightBorder,
                                elevation = ButtonDepth.elevation(),
                                contentPadding = ButtonPadding.listItemAction
                            ) {
                                Text("%,d코인".format(GameConstants.SCOUT_SHIP_BASE_COST),
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (spaceships.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        HorizontalDivider(color = SpaceMid)
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text("보유 우주선", color = GoldAccent, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    items(spaceships, key = { it.id }) { ship ->
                        SpaceshipCard(ship = ship, coins = coins, resources = resources,
                            onUpgrade = { viewModel.upgrade(ship) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceshipCard(
    ship: Spaceship,
    coins: Long,
    resources: List<Resource>,
    onUpgrade: () -> Unit
) {
    val isMaxGrade = ship.grade >= GameConstants.MAX_SPACESHIP_GRADE
    val (upgradeCoinCost, upgradeResourceCost) = GameConstants.spaceshipUpgradeCost(ship.grade)
    val canAffordCoins = coins >= upgradeCoinCost
    val canAffordResources = upgradeResourceCost.all { (type, amount) ->
        (resources.firstOrNull { it.type == type }?.amount ?: 0L) >= amount
    }
    val canUpgrade = !isMaxGrade && canAffordCoins && canAffordResources

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceMid),
        modifier = Modifier.textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(spaceshipImageRes(ship.crewCapacity)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${ship.name}  (등급 ${ship.grade})", color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        MiniStat("탑승", "${ship.crewCapacity}명", TextPrimary)
                        MiniStat("속도", "${ship.speed}", SpaceAccent)
                        MiniStat("적재", "${ship.cargo}", GoldAccent)
                        MiniStat("성공률", "${(ship.successRate * 100).toInt()}%", StatusGreen)
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            // 강화 비용 표시
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                if (isMaxGrade) {
                    Text("최종 등급 (탑승 ${GameConstants.MAX_CREW_CAPACITY}명 달성)", color = GoldAccent,
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                } else {
                    Column {
                        Text("강화 비용", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text("%,d코인".format(upgradeCoinCost),
                            color = if (canAffordCoins) GoldAccent else StatusRed, style = NumericXSmall)
                        if (upgradeResourceCost.isNotEmpty()) {
                            Text(upgradeResourceCost.entries.joinToString(" · ") { "${it.key.displayName}×${it.value}" },
                                color = if (canAffordResources) TextSecondary else StatusRed,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Button(
                        onClick = onUpgrade, enabled = canUpgrade,
                        modifier = Modifier.widthIn(min = ButtonPadding.minWidth),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceAccent, contentColor = SpaceDark),
                        border = ButtonDepth.highlightBorder,
                        elevation = ButtonDepth.elevation(),
                        contentPadding = ButtonPadding.listItemAction
                    ) { Text("강화", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

private fun spaceshipImageRes(crewCapacity: Int): Int = when {
    crewCapacity <= 3 -> R.drawable.spaceship_2
    crewCapacity <= 5 -> R.drawable.spaceship_4
    crewCapacity <= 7 -> R.drawable.spaceship_6
    else -> R.drawable.spaceship_8
}

@Composable
private fun MiniStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, style = LabelTiny)
        Text(value, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
