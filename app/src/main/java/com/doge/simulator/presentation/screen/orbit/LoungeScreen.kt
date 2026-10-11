package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.presentation.component.AutoShowInfoOnce
import com.doge.simulator.presentation.component.DailyResetEffect
import com.doge.simulator.presentation.component.FacilityPanel
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity

// 휴게실 — 정거장 위에 띄우는 시설 창(FacilityPanel). B-01/카드 테이블(=ORBIT 진입)과 일일 리워드 광고 버튼을 둔다.
//
// 휴게실만의 분위기는 방 장면 일러스트(bg_lounge)를 액자처럼 넣어서 낸다 — 사각형 장면을
// 화면 위쪽에 꽉 채우면 너무 튀고, 흐리게 전체에 깔면 어중간하고, 정거장 그림처럼 경계 없이
// 이어 붙이면 네모 경계가 어색해서, 테두리 두른 "휴게실을 들여다보는 창"으로 정리.
//
// ORBIT "입장"은 별도 베팅 화면 대신 베팅 모달(OrbitBetDialog)을 연다. 모달에서 매치가 시작되면
// (uiSnapshot이 생기면) 게임 화면으로 이동한다. 게임·결과 화면은 같은 OrbitViewModel을 써야 하므로
// 뷰모델은 정거장(HQ) 백스택 엔트리에 스코프된 것을 받는다(MainScreen 참고).
@Composable
fun LoungePanel(
    visible: Boolean,
    onClose: () -> Unit,
    onMatchStarted: () -> Unit,
    viewModel: OrbitViewModel
) {
    val coins by viewModel.coins.collectAsState()
    val dailyAdRemaining by viewModel.dailyAdRemaining.collectAsState()
    val message by viewModel.message.collectAsState()
    val betDialogVisible by viewModel.betDialogVisible.collectAsState()
    val activity = LocalContext.current.findActivity()
    var showRules by remember { mutableStateOf(false) }
    AutoShowInfoOnce("orbit_rules", active = visible) { showRules = true }

    // 창은 닫혀 있어도 정거장 화면에 늘 올라가 있으므로, 열릴 때·앱 복귀·자정마다 남은 광고 횟수를 갱신한다.
    DailyResetEffect(active = visible, onRefresh = viewModel::refreshDailyAdRemaining)

    // 베팅 모달에서 매치가 시작되면(uiSnapshot 생성) 게임 화면으로.
    val snapshot by viewModel.uiSnapshot.collectAsState()
    LaunchedEffect(visible, snapshot != null) {
        if (visible && snapshot != null) onMatchStarted()
    }

    if (showRules) {
        OrbitRulesDialog(onDismiss = { showRules = false })
    }
    if (betDialogVisible) {
        OrbitBetDialog(viewModel = viewModel, onDismiss = viewModel::closeBetDialog)
    }
    FacilityPanel(
        visible = visible,
        title = "휴게실",
        onClose = onClose,
        coins = coins,
        onInfo = { showRules = true },
        infoDescription = "ORBIT 규칙"
    ) {
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            message?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    shape = RoundedCornerShape(8.dp),
                    color = SpaceBlue.copy(0.3f)
                ) {
                    Text(
                        it, color = SpaceAccent, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    )
                }
            }

            Column(
                Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // ── 휴게실 장면(액자) ───────────────────────────────
                // 사각형 장면이라 경계 없이 배경에 녹이면 어색하다 — 테두리 두른 액자로
                // "휴게실을 들여다보는 창"처럼 보여준다.
                // 베팅 모달이 떠 있는 동안엔 그림을 숨긴다 — 모달 뒤를 덮는 막이 반투명이라
                // 밝은 장면이 모달 주변으로 비쳐 보여 산만했다(액자 테두리만 남김).
                val frameShape = RoundedCornerShape(12.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(LOUNGE_SCENE_HEIGHT)
                        .clip(frameShape)
                        .border(1.dp, SpaceBlue, frameShape)
                ) {
                    if (!betDialogVisible) {
                        Image(
                            painter = painterResource(R.drawable.bg_lounge),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        // 가장자리만 살짝 어둡게(비네트) — 테두리와 자연스럽게 이어지게.
                        Box(
                            Modifier.matchParentSize().background(
                                Brush.radialGradient(
                                    0.6f to Color.Transparent,
                                    1f to SpaceDark.copy(alpha = 0.55f)
                                )
                            )
                        )
                    }
                }

                // 장면 안에 B-01이 크게 보이므로 말풍선엔 아바타를 붙이지 않는다.
                // 창은 닫혀도 남아 있으므로 열 때마다 새 대사를 뽑는다.
                // 명판 ⓘ가 작아 규칙을 안 보고 바로 입장하는 사람이 많아서, 어느 대사가 뽑히든
                // B-01이 규칙 보는 곳을 한 줄 덧붙인다.
                val line = remember(visible) { B01Lines.lounge.random() + "\n" + B01Lines.LOUNGE_RULES_HINT }
                B01SpeechRow(line = line, showAvatar = false)

                // ── ORBIT 입장 ─────────────────────────────────────
                val cardShape = RoundedCornerShape(12.dp)
                Card(
                    onClick = viewModel::openBetDialog,
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, GoldAccent.copy(0.4f)),
                    modifier = Modifier.fillMaxWidth().textured(shape = cardShape, baseColor = SpaceNavy)
                ) {
                    Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                        FannedCardBacks()
                        Spacer(Modifier.width(Spacing.lg))
                        Column(Modifier.weight(1f)) {
                            Text("ORBIT", color = GoldAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("B-01과 카드 한 판", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("입장 ▶", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                }

                // ── 일일 지원금(리워드 광고) ────────────────────────
                // 단색 버튼은 "샘플 앱" 같다는 피드백으로, 바로 위 ORBIT 입장 카드와 같은 결의
                // 한 줄 카드(아이콘 + 제목/설명 + 오른쪽 행동)로. 테두리는 광고 아이콘의 파랑.
                val canClaimAd = dailyAdRemaining > 0
                Card(
                    onClick = { viewModel.claimDailyAdReward(activity) },
                    enabled = canClaimAd,
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, SpaceLight.copy(alpha = if (canClaimAd) 0.5f else 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (canClaimAd) 1f else 0.6f)
                        .textured(shape = cardShape, baseColor = SpaceNavy)
                ) {
                    Row(
                        Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_ui_ad),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text("일일 지원금", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (canClaimAd) {
                                    "광고 보고 ${"%,d".format(GameConstants.ORBIT_DAILY_AD_REWARD_COINS)}코인 · 오늘 ${dailyAdRemaining}/${GameConstants.ORBIT_DAILY_AD_MAX_COUNT}"
                                } else {
                                    "오늘은 모두 받았어요"
                                },
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            if (canClaimAd) "받기 ▶" else "내일 다시",
                            color = if (canClaimAd) SpaceLight else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 입장 카드 왼쪽 장식 — 카드 뒷면 두 장을 부채꼴로 겹쳐 "카드"로 보이게 한다
// (엠블럼 아이콘만 두면 카드 형태가 없어 아이콘이 붕 떠 보였음).
@Composable
private fun FannedCardBacks() {
    Box(Modifier.size(width = 52.dp, height = 56.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.offset(x = (-7).dp).rotate(-12f)) { OrbitCardBackTile(width = 34.dp, height = 48.dp) }
        Box(Modifier.offset(x = 7.dp).rotate(8f)) { OrbitCardBackTile(width = 34.dp, height = 48.dp) }
    }
}


private val LOUNGE_SCENE_HEIGHT = 190.dp
