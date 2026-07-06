package com.doge.simulator.presentation.screen.hq

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.Astronaut
import com.doge.simulator.domain.model.AstronautSpecialty
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.presentation.viewmodel.AstronautViewModel
import com.doge.simulator.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstronautScreen(
    onBack: () -> Unit,
    viewModel: AstronautViewModel = hiltViewModel()
) {
    val astronauts by viewModel.astronauts.collectAsState()
    val researchLab by viewModel.researchLab.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val message by viewModel.message.collectAsState()

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
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp), color = SpaceBlue.copy(0.3f)) {
                    Text(it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }

            // 현황 헤더
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip("보유", "${astronauts.size}/${researchLab.maxAstronauts}명", TextPrimary)
                InfoChip("훈련 슬롯", "${astronauts.count { it.status == AstronautStatus.TRAINING }}/${researchLab.maxTrainingSlots}", SpaceAccent)
                InfoChip("보유 코인", "%,d".format(coins), GoldAccent)
            }

            HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(horizontal = 16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 고용 섹션
                item {
                    Text("우주인 고용", color = GoldAccent, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val hireCost = GameConstants.ASTRONAUT_BASE_HIRE_COST +
                            astronauts.size * GameConstants.ASTRONAUT_HIRE_COST_PER_EXISTING
                    HireSection(hireCost = hireCost, canHire = astronauts.size < researchLab.maxAstronauts,
                        onHire = { viewModel.hire(it) })
                }
                // 구분선
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = SpaceMid)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("보유 우주인 (${astronauts.size}명)", color = GoldAccent,
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (astronauts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
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
                            onTrainAdvanced = { viewModel.train(astronaut, true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HireSection(hireCost: Long, canHire: Boolean, onHire: (AstronautSpecialty) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AstronautSpecialty.entries.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { specialty ->
                    OutlinedButton(
                        onClick = { onHire(specialty) },
                        enabled = canHire,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpaceAccent),
                        border = BorderStroke(1.dp, SpaceAccent.copy(if (canHire) 0.5f else 0.2f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(specialty.icon, fontSize = 18.sp)
                            Text(specialty.displayName, style = MaterialTheme.typography.labelSmall,
                                maxLines = 1)
                            Text("%,d코인".format(hireCost), color = GoldAccent,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
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
    onTrainAdvanced: () -> Unit
) {
    val (statusColor, statusLabel) = when (astronaut.status) {
        AstronautStatus.IDLE -> StatusGreen to "대기 중"
        AstronautStatus.DEPLOYED -> SpaceAccent to "탐사 중"
        AstronautStatus.TRAINING -> StatusYellow to "훈련 중"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceNavy.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, SpaceMid),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(astronaut.specialty.icon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(astronaut.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(4.dp), color = statusColor.copy(0.15f)) {
                            Text(statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(astronaut.specialty.displayName, color = SpaceAccent,
                            style = MaterialTheme.typography.labelSmall)
                        Text("Lv.${astronaut.level}", color = GoldAccent,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (astronaut.status == AstronautStatus.TRAINING && astronaut.trainingEndTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val remaining = (astronaut.trainingEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
                Text("훈련 완료까지: ${formatHours(remaining)}", color = StatusYellow,
                    style = MaterialTheme.typography.labelSmall)
            }

            if (astronaut.status == AstronautStatus.IDLE) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTrainBasic,
                        enabled = trainingSlotAvailable && coins >= GameConstants.BASIC_TRAINING_COST_COINS,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, SpaceMid),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("기초 훈련", style = MaterialTheme.typography.labelSmall)
                            Text("4시간 / ${"%,d".format(GameConstants.BASIC_TRAINING_COST_COINS)}코인",
                                style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                        }
                    }
                    Button(
                        onClick = onTrainAdvanced,
                        enabled = trainingSlotAvailable && coins >= GameConstants.ADVANCED_TRAINING_COST_COINS,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceBlue, contentColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("심화 훈련", style = MaterialTheme.typography.labelSmall)
                            Text("12시간 / ${"%,d".format(GameConstants.ADVANCED_TRAINING_COST_COINS)}코인",
                                style = MaterialTheme.typography.labelSmall, color = TextDisabled)
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
