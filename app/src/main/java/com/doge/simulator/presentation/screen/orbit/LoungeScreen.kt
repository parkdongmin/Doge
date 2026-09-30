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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.presentation.component.DogeTopBar
import com.doge.simulator.presentation.component.PixelButton
import com.doge.simulator.presentation.component.NightSkyBackground
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity

// 휴게실 — 정거장 하위 진입점. B-01/카드 테이블(=ORBIT 진입)과 일일 리워드 광고 버튼을 둔다.
//
// 틀(상단 바 / 보유 현황 줄 / textured 카드)은 격납고·우주인 센터·연구소와 똑같이 맞춘다.
// 휴게실만의 분위기는 방 장면 일러스트(bg_lounge)를 액자처럼 넣어서 낸다 — 사각형 장면을
// 화면 위쪽에 꽉 채우면 너무 튀고, 흐리게 전체에 깔면 어중간하고, 정거장 그림처럼 경계 없이
// 이어 붙이면 네모 경계가 어색해서, 테두리 두른 "휴게실을 들여다보는 창"으로 정리.
//
// ORBIT "입장"은 별도 베팅 화면 대신 베팅 모달(OrbitBetDialog)을 연다. 모달에서 매치가 시작되면
// (uiSnapshot이 생기면) 게임 화면으로 이동한다.
@Composable
fun LoungeScreen(
    onBack: () -> Unit,
    onMatchStarted: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val coins by viewModel.coins.collectAsState()
    val dailyAdRemaining by viewModel.dailyAdRemaining.collectAsState()
    val message by viewModel.message.collectAsState()
    val betDialogVisible by viewModel.betDialogVisible.collectAsState()
    val activity = LocalContext.current.findActivity()
    var showRules by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshDailyAdRemaining() }

    // 베팅 모달에서 매치가 시작되면(uiSnapshot 생성) 게임 화면으로.
    val snapshot by viewModel.uiSnapshot.collectAsState()
    LaunchedEffect(snapshot != null) {
        if (snapshot != null) onMatchStarted()
    }

    Scaffold(
        topBar = {
            DogeTopBar(
                title = "휴게실",
                onBack = onBack,
                onInfo = { showRules = true },
                infoDescription = "ORBIT 규칙"
            )
        },
        containerColor = SpaceDark
    ) { padding ->
        if (showRules) {
            OrbitRulesDialog(onDismiss = { showRules = false })
        }
        if (betDialogVisible) {
            OrbitBetDialog(viewModel = viewModel, onDismiss = viewModel::closeBetDialog)
        }
        NightSkyBackground(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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

                // ── 보유 현황 ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("코인", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Text("%,d".format(coins), color = GoldAccent, style = NumericXSmall)
                    }
                }

                HorizontalDivider(color = SpaceMid, modifier = Modifier.padding(horizontal = Spacing.lg))

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
                    val line = remember { B01Lines.lounge.random() }
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
                    // 따로 카드 섹션을 만들 만큼의 내용이 아니라 버튼 하나로. 남은 횟수는 버튼에 표시.
                    PixelButton(
                        text = if (dailyAdRemaining > 0) {
                            "광고 보고 지원금 받기 (${dailyAdRemaining}/${GameConstants.ORBIT_DAILY_AD_MAX_COUNT})"
                        } else {
                            "오늘 지원금 소진"
                        },
                        onClick = { viewModel.claimDailyAdReward(activity) },
                        enabled = dailyAdRemaining > 0,
                        contentPadding = ButtonPadding.fullWidthCta,
                        modifier = Modifier.fillMaxWidth()
                    )
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
