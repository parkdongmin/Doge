package com.doge.simulator.presentation.screen.hq

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResearchField
import com.doge.simulator.domain.model.ResearchLab
import com.doge.simulator.domain.model.Resource
import com.doge.simulator.presentation.viewmodel.ResearchLabViewModel
import com.doge.simulator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchLabScreen(
    onBack: () -> Unit,
    viewModel: ResearchLabViewModel = hiltViewModel()
) {
    val researchLab by viewModel.researchLab.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val message by viewModel.message.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("연구소", color = GoldAccent, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceNavy)
            )
        },
        containerColor = SpaceDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            message?.let {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp), color = SpaceBlue.copy(0.3f)) {
                    Text(it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp))
                }
            }

            // 보유 코인
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("총 연구 레벨: ${researchLab.totalLevel}", color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
                Text("%,d 코인".format(coins), color = GoldAccent, style = NumericSmall)
            }

            HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))

            ResearchField.entries.forEach { field ->
                ResearchFieldCard(
                    field = field,
                    currentLevel = researchLab.getLevel(field),
                    researchLab = researchLab,
                    coins = coins,
                    resources = resources,
                    onUpgrade = { viewModel.upgrade(field) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResearchFieldCard(
    field: ResearchField,
    currentLevel: Int,
    researchLab: ResearchLab,
    coins: Long,
    resources: List<Resource>,
    onUpgrade: () -> Unit
) {
    val maxLevel = ResearchLab.maxLevel(field)
    val isMaxLevel = maxLevel != null && currentLevel >= maxLevel
    val (coinCost, resourceCost) = GameConstants.researchUpgradeCost(currentLevel)
    val canAffordCoins = coins >= coinCost
    val canAffordResources = resourceCost.all { (type, amount) ->
        (resources.firstOrNull { it.type == type }?.amount ?: 0L) >= amount
    }
    val canUpgrade = !isMaxLevel && canAffordCoins && canAffordResources

    // 레벨별 효과 설명
    val effectDescription = when (field) {
        ResearchField.EXPLORATION_TECH -> when {
            currentLevel <= 2 -> "현재: 광물·행성 탐사 가능"
            currentLevel == 3 -> "Lv.3 달성 시: 유적 탐사 해금"
            !isMaxLevel -> "Lv.${maxLevel} 달성 시: 외계 문명 탐사 해금"
            else -> "현재: 모든 탐사 분야 해금 완료 (최대 레벨)"
        }
        ResearchField.CELESTIAL_ANALYSIS -> {
            // 실제 발견 확률(CompleteExpeditionUseCase와 동일 공식)은 80%로 코어스되므로,
            // 상한 없이 계속 오르는 것처럼 보이는 원시 보너스(레벨×3%) 대신 실제 확률을 그대로 표시
            val celestialBonus = currentLevel * GameConstants.PLANET_DISCOVERY_CELESTIAL_BONUS_PER_LEVEL
            val normalChance = (GameConstants.PLANET_DISCOVERY_BASE_CHANCE + celestialBonus).coerceIn(0f, 0.8f)
            val planetChance = (GameConstants.PLANET_DISCOVERY_BASE_CHANCE +
                    GameConstants.PLANET_DISCOVERY_PLANET_CATEGORY_BONUS + celestialBonus).coerceIn(0f, 0.8f)
            "행성 슬롯 ${researchLab.maxPlanetSlots}개 · 행성 발견 확률 ${(normalChance * 100).toInt()}%(행성 탐사 시 ${(planetChance * 100).toInt()}%)"
        }
        ResearchField.HR_MANAGEMENT ->
            "우주인 최대 ${researchLab.maxAstronauts}명 · 훈련 슬롯 ${researchLab.maxTrainingSlots}개"
        ResearchField.SPACE_ENGINEERING ->
            "우주선 최대 ${researchLab.maxSpaceships}척 보유 가능"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(0.85f)),
        border = BorderStroke(1.dp, SpaceBlue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(field.icon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(field.displayName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                    Text(field.description, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = GoldAccent.copy(0.15f),
                    border = BorderStroke(1.dp, GoldAccent.copy(0.3f))) {
                    Text("Lv.$currentLevel", color = GoldAccent, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp),
                color = SpaceBlue.copy(0.2f)) {
                Text(effectDescription, color = SpaceAccent, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                if (isMaxLevel) {
                    Text("최대 레벨 달성", color = GoldAccent, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                } else {
                    Column {
                        Text("Lv.${currentLevel} → Lv.${currentLevel + 1} 비용",
                            color = TextDisabled, style = MaterialTheme.typography.labelSmall)
                        Text("%,d 코인".format(coinCost),
                            color = if (canAffordCoins) GoldAccent else StatusRed, style = NumericXSmall)
                        if (resourceCost.isNotEmpty()) {
                            Text(resourceCost.entries.joinToString(" · ") { "${it.key.displayName}×${it.value}" },
                                color = if (canAffordResources) TextSecondary else StatusRed,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Button(
                    onClick = onUpgrade, enabled = canUpgrade,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (canUpgrade) SpaceAccent else SpaceMid,
                        contentColor = SpaceDark),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(if (isMaxLevel) "MAX" else "연구",
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
