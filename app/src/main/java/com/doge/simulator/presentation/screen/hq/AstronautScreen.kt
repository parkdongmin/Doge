package com.doge.simulator.presentation.screen.hq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.doge.simulator.domain.model.RecruitmentCandidate
import com.doge.simulator.domain.model.RecruitmentPool
import com.doge.simulator.presentation.viewmodel.AstronautViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity
import kotlinx.coroutines.delay

private val gradeColor = mapOf(
    AstronautGrade.INTERN to Color(0xFF9EA3A8),
    AstronautGrade.REGULAR to Color(0xFF5DBF7A),
    AstronautGrade.SENIOR to Color(0xFF5B9CF6),
    AstronautGrade.VETERAN to Color(0xFFB07FE0),
    AstronautGrade.LEGEND to Color(0xFFE8A84C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstronautScreen(
    onBack: () -> Unit,
    viewModel: AstronautViewModel = hiltViewModel()
) {
    val astronauts by viewModel.astronauts.collectAsState()
    val researchLab by viewModel.researchLab.collectAsState()
    val recruitmentPool by viewModel.recruitmentPool.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val message by viewModel.message.collectAsState()
    val activity = LocalContext.current.findActivity()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("우주인 센터", color = GoldAccent, style = MaterialTheme.typography.titleMedium) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 상태 메시지
            message?.let {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    shape = RoundedCornerShape(8.dp), color = SpaceBlue.copy(0.3f)) {
                    Text(it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
                }
            }

            // 현황 헤더
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip("보유", "${astronauts.size}/${researchLab.maxAstronauts}명", TextPrimary)
                InfoChip("훈련 슬롯", "${astronauts.count { it.status == AstronautStatus.TRAINING }}/${researchLab.maxTrainingSlots}", SpaceAccent)
                InfoChip("보유 코인", "%,d".format(coins), GoldAccent)
            }

            HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(horizontal = Spacing.lg))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // 모집 센터
                item {
                    Text("모집 센터", color = GoldAccent, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
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
                    Text("보유 우주인 (${astronauts.size}명)", color = GoldAccent,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
            TextButton(onClick = onRefreshAd, contentPadding = ButtonPadding.textInline) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Image(
                        painter = painterResource(R.drawable.ic_ui_ad),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("광고 보고 새로고침", color = SpaceAccent, style = MaterialTheme.typography.labelSmall)
                }
            }
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
        modifier = Modifier.fillMaxWidth().textured(shape = RoundedCornerShape(10.dp), baseColor = SpaceNavy.copy(alpha = 0.7f))
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
            Button(
                onClick = onHire,
                enabled = canHire,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = SpaceDark),
                border = ButtonDepth.highlightBorder,
                elevation = ButtonDepth.elevation(),
                contentPadding = ButtonPadding.listItemAction
            ) {
                Text("%,d".format(cost ?: 0L), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AstronautCard(
    astronaut: Astronaut,
    coins: Long,
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
        modifier = Modifier.fillMaxWidth().textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f))
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
                        TextButton(onClick = onSkipWaitAd, contentPadding = ButtonPadding.textInline) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Image(
                                    painter = painterResource(R.drawable.ic_ui_ad),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("광고로 4시간 당기기", color = SpaceAccent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (astronaut.status == AstronautStatus.IDLE) {
                Spacer(modifier = Modifier.height(Spacing.md))
                if (atCap) {
                    Text("이 등급의 숙련도 한계에 도달했습니다", color = TextDisabled,
                        style = MaterialTheme.typography.labelSmall)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(
                            onClick = onTrainBasic,
                            enabled = trainingSlotAvailable && coins >= GameConstants.BASIC_TRAINING_COST_COINS,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, SpaceMid),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("기초 훈련 (+${GameConstants.BASIC_TRAINING_PROFICIENCY_GAIN})", style = MaterialTheme.typography.labelSmall)
                                Text("4시간 / ${"%,d".format(GameConstants.BASIC_TRAINING_COST_COINS)}코인",
                                    style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                            }
                        }
                        Button(
                            onClick = onTrainAdvanced,
                            enabled = trainingSlotAvailable && coins >= GameConstants.ADVANCED_TRAINING_COST_COINS,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = SpaceDark),
                            border = ButtonDepth.highlightBorder,
                            elevation = ButtonDepth.elevation(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("심화 훈련 (+${GameConstants.ADVANCED_TRAINING_PROFICIENCY_GAIN})", style = MaterialTheme.typography.labelSmall)
                                Text("12시간 / ${"%,d".format(GameConstants.ADVANCED_TRAINING_COST_COINS)}코인",
                                    style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

private fun formatHours(ms: Long): String {
    val h = ms / 3_600_000L
    val m = (ms % 3_600_000L) / 60_000L
    return if (h > 0) "${h}시간 ${m}분" else "${m}분"
}