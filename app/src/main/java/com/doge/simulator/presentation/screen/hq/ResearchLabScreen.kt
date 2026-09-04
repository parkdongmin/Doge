package com.doge.simulator.presentation.screen.hq

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.ExpeditionCategory
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
        ) {
            // 보유 코인
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("총 연구 레벨: ${researchLab.totalLevel}", color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
                Text("%,d 코인".format(coins), color = GoldAccent, style = NumericSmall)
            }

            HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(horizontal = Spacing.lg))
            Spacer(modifier = Modifier.height(Spacing.sm))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ResearchField.entries.forEach { field ->
                    ResearchFieldCard(
                        field = field,
                        currentLevel = researchLab.getLevel(field),
                        researchLab = researchLab,
                        coins = coins,
                        resources = resources,
                        onUpgrade = { viewModel.upgrade(field) }
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }

                Spacer(modifier = Modifier.height(Spacing.lg))
            }
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

    // 레벨별 효과 설명 — 최대 레벨이 아니면 "현재값 → 다음값"으로 다음 연구 결과를 미리 보여줌
    val nextLab = when (field) {
        ResearchField.EXPLORATION_TECH -> researchLab.copy(explorationTechLevel = currentLevel + 1)
        ResearchField.CELESTIAL_ANALYSIS -> researchLab.copy(celestialAnalysisLevel = currentLevel + 1)
        ResearchField.HR_MANAGEMENT -> researchLab.copy(hrLevel = currentLevel + 1)
        ResearchField.SPACE_ENGINEERING -> researchLab.copy(engineeringLevel = currentLevel + 1)
    }
    // 변화가 있으면 "지금 → 다음", 없거나 최대 레벨이면 현재값만
    fun step(now: Int, next: Int, unit: String) =
        if (isMaxLevel || now == next) "$now$unit" else "$now → $next$unit"
    // 실제 발견 확률(CompleteExpeditionUseCase와 동일 공식, 80%로 코어스)
    fun discoveryChance(lab: ResearchLab, planetCategory: Boolean): Int {
        val extra = if (planetCategory) GameConstants.PLANET_DISCOVERY_PLANET_CATEGORY_BONUS else 0f
        val bonus = lab.celestialAnalysisLevel * GameConstants.PLANET_DISCOVERY_CELESTIAL_BONUS_PER_LEVEL
        return ((GameConstants.PLANET_DISCOVERY_BASE_CHANCE + extra + bonus).coerceIn(0f, 0.8f) * 100).toInt()
    }

    val effectDescription = when (field) {
        ResearchField.EXPLORATION_TECH -> {
            // 매직넘버(3, 6) 대신 실제 해금 기준(ExpeditionCategory.researchLevelRequired)에서
            // 직접 끌어온다 — 카테고리 해금 레벨이 나중에 바뀌어도 이 문구가 안 따라가는
            // 일이 없게. 예전엔 Lv.0~2 구간에 "다음 해금" 미리보기가 아예 안 뜨고,
            // 정확히 Lv.3에 "달성 시"라는 미래형 문구가 뜨는(이미 달성했는데) 버그가 있었음
            val ruinsLv = ExpeditionCategory.RUINS.researchLevelRequired
            val alienLv = ExpeditionCategory.ALIEN_CIVILIZATION.researchLevelRequired
            when {
                currentLevel < ruinsLv -> "현재: 광물·행성 탐사 가능 · Lv.$ruinsLv 달성 시 유적 탐사 해금"
                currentLevel < alienLv -> "현재: 유적 탐사까지 가능 · Lv.$alienLv 달성 시 외계 문명 탐사 해금"
                else -> "현재: 모든 탐사 분야 해금 완료 (최대 레벨)"
            }
        }
        ResearchField.CELESTIAL_ANALYSIS ->
            "행성 슬롯 ${step(researchLab.maxPlanetSlots, nextLab.maxPlanetSlots, "개")} · " +
                "발견 확률 ${step(discoveryChance(researchLab, false), discoveryChance(nextLab, false), "%")}" +
                "(행성 탐사 시 ${step(discoveryChance(researchLab, true), discoveryChance(nextLab, true), "%")})"
        ResearchField.HR_MANAGEMENT ->
            "우주인 ${step(researchLab.maxAstronauts, nextLab.maxAstronauts, "명")} · " +
                "훈련 슬롯 ${step(researchLab.maxTrainingSlots, nextLab.maxTrainingSlots, "개")}"
        ResearchField.SPACE_ENGINEERING ->
            "우주선 ${step(researchLab.maxSpaceships, nextLab.maxSpaceships, "척")} 보유 가능"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)
            .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceBlue)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(field.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(IconGlyphSize.large.value.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(field.displayName, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(field.description, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = GoldAccent.copy(0.15f),
                    border = BorderStroke(1.dp, GoldAccent.copy(0.3f))) {
                    Text("Lv.$currentLevel", color = GoldAccent, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp),
                color = SpaceBlue.copy(0.2f)) {
                Text(effectDescription, color = SpaceAccent, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                if (isMaxLevel) {
                    Text("최대 레벨 달성", color = GoldAccent, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold)
                } else {
                    Column {
                        Text("Lv.${currentLevel} → Lv.${currentLevel + 1} 비용",
                            color = TextSecondary, style = MaterialTheme.typography.labelSmall)
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
                    modifier = Modifier.widthIn(min = ButtonPadding.minWidth),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (canUpgrade) SpaceAccent else SpaceMid,
                        contentColor = SpaceDark),
                    border = ButtonDepth.highlightBorder,
                    elevation = ButtonDepth.elevation(),
                    contentPadding = ButtonPadding.ctaInRow
                ) {
                    Text(if (isMaxLevel) "MAX" else "연구",
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
