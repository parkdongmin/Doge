package com.doge.simulator.presentation.screen.hq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautGrade
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.ResourceType
import com.doge.simulator.domain.model.RecruitmentCandidate
import com.doge.simulator.domain.model.RecruitmentPool
import com.doge.simulator.presentation.component.CrewInfoContent
import com.doge.simulator.presentation.component.InfoDialog
import com.doge.simulator.presentation.viewmodel.AstronautViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.presentation.component.GameButton
import com.doge.simulator.presentation.component.GameButtonStyle
import com.doge.simulator.util.findActivity
import com.doge.simulator.presentation.component.FacilityPanel
import com.doge.simulator.presentation.component.PanelSectionHeader
import kotlinx.coroutines.delay

private val gradeColor = mapOf(
    AstronautGrade.INTERN to Color(0xFF9EA3A8),
    AstronautGrade.REGULAR to Color(0xFF5DBF7A),
    AstronautGrade.SENIOR to Color(0xFF5B9CF6),
    AstronautGrade.VETERAN to Color(0xFFB07FE0),
    AstronautGrade.LEGEND to Color(0xFFE8A84C)
)

// 정거장 화면 위에 띄우는 우주인 센터 창(FacilityPanel 참고).
@Composable
fun AstronautPanel(
    visible: Boolean,
    onClose: () -> Unit,
    viewModel: AstronautViewModel = hiltViewModel()
) {
    val coins by viewModel.coins.collectAsState()
    var showInfo by remember { mutableStateOf(false) }

    if (showInfo) {
        InfoDialog(title = "우주인 안내", onDismiss = { showInfo = false }) {
            CrewInfoContent(showGrades = true)
        }
    }
    FacilityPanel(
        visible = visible,
        title = "우주인 센터",
        onClose = onClose,
        coins = coins,
        onInfo = { showInfo = true },
        infoDescription = "우주인 안내"
    ) {
        AstronautContent(viewModel, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun AstronautContent(viewModel: AstronautViewModel, modifier: Modifier) {
    val astronauts by viewModel.astronauts.collectAsState()
    val researchLab by viewModel.researchLab.collectAsState()
    val recruitmentPool by viewModel.recruitmentPool.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val message by viewModel.message.collectAsState()
    val activity = LocalContext.current.findActivity()

    Column(modifier = modifier) {
        // 상태 메시지
        message?.let {
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                shape = RoundedCornerShape(8.dp), color = SpaceBlue.copy(0.3f)) {
                Text(it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // 모집 센터
            item {
                PanelSectionHeader("모집 센터")
                Spacer(modifier = Modifier.height(Spacing.sm))
                RecruitmentSection(
                    pool = recruitmentPool,
                    coins = coins,
                    canHire = astronauts.size < researchLab.maxAstronauts,
                    onHire = { viewModel.hireFromPool(it) },
                    onRefreshAd = { viewModel.refreshPoolWithAd(activity) }
                )
            }
            // 구분선
            item {
                Spacer(modifier = Modifier.height(Spacing.xs))
                HorizontalDivider(color = SpaceMid)
                Spacer(modifier = Modifier.height(Spacing.xs))
                // 보유·훈련 슬롯 현황은 따로 한 줄을 두지 않고 제목 오른쪽에(격납고와 같은 방식).
                PanelSectionHeader(
                    "보유 우주인",
                    trailing = "${astronauts.size}/${researchLab.maxAstronauts}명 · 훈련 " +
                        "${astronauts.count { it.status == AstronautStatus.TRAINING }}/${researchLab.maxTrainingSlots}"
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
            if (astronauts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxl),
                        contentAlignment = Alignment.Center) {
                        Text("아직 고용한 우주인이 없습니다", color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(astronauts, key = { it.id }) { astronaut ->
                    AstronautCard(
                        astronaut = astronaut,
                        coins = coins,
                        resources = resources,
                        trainingSlotAvailable = astronauts.count { it.status == AstronautStatus.TRAINING } < researchLab.maxTrainingSlots,
                        onTrainBasic = { viewModel.train(astronaut, false) },
                        onTrainAdvanced = { viewModel.train(astronaut, true) },
                        onSkipWaitAd = { viewModel.skipTrainingWait(astronaut, activity) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecruitmentSection(
    pool: RecruitmentPool,
    coins: Long,
    canHire: Boolean,
    onHire: (Int) -> Unit,
    onRefreshAd: () -> Unit
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }
    val remaining = (pool.nextRefreshTime - now).coerceAtLeast(0L)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("다음 자동 새로고침: ${formatHours(remaining)}", color = TextSecondary,
                style = MaterialTheme.typography.labelSmall)
            GameButton(
                text = "광고 보고 새로고침",
                onClick = onRefreshAd,
                style = GameButtonStyle.Primary,
                leadingIcon = R.drawable.ic_ui_ad
            )
        }
        val slots = if (pool.slots.isEmpty()) List(GameConstants.RECRUITMENT_POOL_SIZE) { null } else pool.slots
        slots.forEachIndexed { index, candidate ->
            RecruitmentCandidateCard(
                candidate = candidate,
                cost = candidate?.hireCost(),
                canHire = canHire && candidate != null && coins >= candidate.hireCost(),
                onHire = { onHire(index) }
            )
        }
    }
}

@Composable
private fun RecruitmentCandidateCard(
    candidate: RecruitmentCandidate?,
    cost: Long?,
    canHire: Boolean,
    onHire: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, candidate?.let { gradeColor.getValue(it.grade) }?.copy(alpha = 0.5f) ?: SpaceMid),
        modifier = Modifier.fillMaxWidth().textured(shape = RoundedCornerShape(10.dp), baseColor = SpaceNavy)
    ) {
        if (candidate == null) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.lg), contentAlignment = Alignment.Center) {
                Text("영입 완료 · 다음 새로고침을 기다려주세요", color = TextDisabled,
                    style = MaterialTheme.typography.labelSmall)
            }
            return@Card
        }
        Row(
            modifier = Modifier.padding(Spacing.md).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(candidate.specialty.characterImageRes(candidate.grade)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(candidate.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                    Surface(shape = RoundedCornerShape(4.dp), color = gradeColor.getValue(candidate.grade).copy(alpha = 0.18f)) {
                        Text(candidate.grade.displayName, color = gradeColor.getValue(candidate.grade),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs))
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text("${candidate.specialty.displayName} · 숙련도 ${candidate.proficiency}",
                    color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            // 코인을 쓰는 영입 — 격납고 구매 버튼과 같은 금색, 금액은 코인 아이콘 + 숫자.
            GameButton(
                text = "",
                coinAmount = cost ?: 0L,
                onClick = onHire,
                enabled = canHire,
                style = GameButtonStyle.Gold
            )
        }
    }
}

@Composable
private fun AstronautCard(
    astronaut: Astronaut,
    coins: Long,
    resources: Map<ResourceType, Long>,
    trainingSlotAvailable: Boolean,
    onTrainBasic: () -> Unit,
    onTrainAdvanced: () -> Unit,
    onSkipWaitAd: () -> Unit
) {
    val (statusColor, statusLabel) = when (astronaut.status) {
        AstronautStatus.IDLE -> StatusGreen to "대기 중"
        AstronautStatus.DEPLOYED -> SpaceAccent to "탐사 중"
        AstronautStatus.TRAINING -> StatusYellow to "훈련 중"
    }
    val atCap = astronaut.proficiency >= astronaut.grade.proficiencyCap

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceMid),
        modifier = Modifier.fillMaxWidth().textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy)
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(astronaut.specialty.characterImageRes(astronaut.grade)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(astronaut.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(0.15f)) {
                            Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs))
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(4.dp), color = gradeColor.getValue(astronaut.grade).copy(alpha = 0.18f)) {
                            Text(astronaut.grade.displayName, color = gradeColor.getValue(astronaut.grade),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs))
                        }
                        Text(astronaut.specialty.displayName, color = SpaceAccent,
                            style = MaterialTheme.typography.labelSmall)
                        Text("숙련도 ${astronaut.proficiency}/${astronaut.grade.proficiencyCap}", color = GoldAccent,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (astronaut.status == AstronautStatus.TRAINING && astronaut.trainingEndTime != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                val remaining = (astronaut.trainingEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("훈련 완료까지: ${formatHours(remaining)}", color = StatusYellow,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    if (remaining > 60_000L) {
                        GameButton(
                            text = "광고로 4시간 당기기",
                            onClick = onSkipWaitAd,
                            style = GameButtonStyle.Primary,
                            leadingIcon = R.drawable.ic_ui_ad
                        )
                    }
                }
            }

            if (astronaut.status == AstronautStatus.IDLE) {
                Spacer(modifier = Modifier.height(Spacing.md))
                if (atCap) {
                    Text("이 등급의 숙련도 한계에 도달했습니다", color = TextDisabled,
                        style = MaterialTheme.typography.labelSmall)
                } else {
                    val hasResources = { cost: Map<ResourceType, Int> ->
                        cost.all { (type, amount) -> (resources[type] ?: 0L) >= amount }
                    }
                    val canBasic = hasResources(GameConstants.BASIC_TRAINING_RESOURCE_COST)
                    val canAdvanced = hasResources(GameConstants.ADVANCED_TRAINING_RESOURCE_COST)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        // 카드 한 장에 강조색 버튼은 최대 하나 — 훈련은 두 개라 둘 다 남색, 금액만 금색으로 강조.
                        GameButton(
                            text = "기초 훈련 +${GameConstants.BASIC_TRAINING_PROFICIENCY_GAIN}",
                            onClick = onTrainBasic,
                            enabled = trainingSlotAvailable && canBasic && coins >= GameConstants.BASIC_TRAINING_COST_COINS,
                            modifier = Modifier.weight(1f),
                            style = GameButtonStyle.Neutral,
                            subText = "4시간",
                            subCoinAmount = GameConstants.BASIC_TRAINING_COST_COINS
                        )
                        GameButton(
                            text = "심화 훈련 +${GameConstants.ADVANCED_TRAINING_PROFICIENCY_GAIN}",
                            onClick = onTrainAdvanced,
                            enabled = trainingSlotAvailable && canAdvanced && coins >= GameConstants.ADVANCED_TRAINING_COST_COINS,
                            modifier = Modifier.weight(1f),
                            style = GameButtonStyle.Neutral,
                            subText = "12시간",
                            subCoinAmount = GameConstants.ADVANCED_TRAINING_COST_COINS
                        )
                    }
                    // 버튼 안은 시간·코인만으로도 꽉 차서 자원 비용은 아래 한 줄로 (격납고 강화 비용 표기와 같은 방식)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        "기초 " + GameConstants.BASIC_TRAINING_RESOURCE_COST.costLabel() +
                            " · 심화 " + GameConstants.ADVANCED_TRAINING_RESOURCE_COST.costLabel(),
                        color = if (canBasic) TextSecondary else StatusRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun Map<ResourceType, Int>.costLabel(): String =
    entries.joinToString(" · ") { "${it.key.displayName}×${it.value}" }

private fun formatHours(ms: Long): String {
    val h = ms / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    return if (h > 0) "${h}시간 ${m}분" else "${m}분"
}
