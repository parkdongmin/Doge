package com.doge.simulator.presentation.screen.hq

import androidx.annotation.DrawableRes
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.doge.simulator.R
import com.doge.simulator.presentation.navigation.NavRoutes
import com.doge.simulator.presentation.screen.orbit.LoungePanel
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.presentation.component.PixelIcons
import com.doge.simulator.presentation.component.TabHeader
import com.doge.simulator.presentation.component.TabHeaderPadding
import com.doge.simulator.presentation.component.TabSideMargin
import com.doge.simulator.presentation.component.TabSectionHeader

@Composable
fun HQScreen(
    navController: NavController,
    // ORBIT 게임·결과 화면과 같이 쓰는 매치 상태 — 정거장(HQ) 엔트리 스코프(MainScreen에서 넘겨줌).
    orbitViewModel: OrbitViewModel
) {
    // 우주인 센터·격납고·연구소·휴게실은 새 화면으로 넘기지 않고 정거장 위에 창으로 연다(FacilityPanel 참고).
    // 한 번에 하나만 열리므로 열린 시설 이름 하나로 관리한다.
    var openFacility by rememberSaveable { mutableStateOf<String?>(null) }

    // 세로가 짧은 화면(폴드 펼침 등)에선 휴게실 카드가 아래로 잘렸다 — 정거장 그림 높이를 화면 높이에
    // 맞춰 줄이고(최대 250dp), 그래도 넘치면 스크롤되게 한다.
    BoxWithConstraints(Modifier.fillMaxSize().background(SpaceDark).statusBarsPadding()) {
        val heroHeight = (maxHeight * 0.3f).coerceIn(160.dp, 250.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── 제목 ───────────────────────────────────────────────────
            TabHeader(
                title = "정거장",
                subtitle = "시설을 운영해 탐사 역량을 키우세요",
                modifier = Modifier.padding(TabHeaderPadding)
            )

            // ── 정거장 이미지 ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .drawBehind {
                        // 중앙 방사형
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A3A6B).copy(alpha = 0.7f),
                                    Color(0xFF0D1F3C).copy(alpha = 0.4f),
                                    Color(0xFF00020E)
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.55f
                            )
                        )
                        // 위 경계 페이드
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF00020E), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.35f
                            )
                        )
                        // 아래 경계 페이드
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF00020E)),
                                startY = size.height * 0.65f,
                                endY = size.height
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.bg_space_station),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 32.dp, vertical = Spacing.sm)
                )
            }

            // ── 시설 목록 ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TabSideMargin, vertical = Spacing.lg)
            ) {
                HQFacilityCard(
                    iconRes = R.drawable.character_1,
                    title = "우주인 센터",
                    description = "우주인 고용 및 훈련 관리",
                    onClick = { openFacility = FACILITY_ASTRONAUT }
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                HQFacilityCard(
                    iconRes = R.drawable.spaceship_2,
                    title = "격납고",
                    description = "우주선 구매 및 강화",
                    onClick = { openFacility = FACILITY_HANGAR }
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                HQFacilityCard(
                    iconRes = R.drawable.ic_space_station_research,
                    title = "연구소",
                    description = "탐사 기술·천체 분석·인사·공학 연구",
                    onClick = { openFacility = FACILITY_RESEARCH }
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                HQFacilityCard(
                    iconRes = R.drawable.ic_space_station_lounge,
                    title = "휴게실",
                    description = "B-01과 카드게임 ORBIT 한 판",
                    onClick = { openFacility = FACILITY_LOUNGE }
                )
            }
        }

        val closeFacility = { openFacility = null }
        AstronautPanel(visible = openFacility == FACILITY_ASTRONAUT, onClose = closeFacility)
        HangarPanel(visible = openFacility == FACILITY_HANGAR, onClose = closeFacility)
        ResearchLabPanel(visible = openFacility == FACILITY_RESEARCH, onClose = closeFacility)
        LoungePanel(
            visible = openFacility == FACILITY_LOUNGE,
            onClose = closeFacility,
            onMatchStarted = { navController.navigate(NavRoutes.OrbitGame.route) { launchSingleTop = true } },
            viewModel = orbitViewModel
        )
    }
}

@Composable
private fun HQFacilityCard(
    @DrawableRes iconRes: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .textured(shape = RoundedCornerShape(12.dp), baseColor = SpaceNavy.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SpaceBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                // 가로 56 × 세로 44 — 가로로 긴 그림(격납고 우주선 약 1.8:1)이 정사각 칸에선 세로 24dp로 납작하게
                // 줄어 혼자 작아 보였다. 칸을 가로로 넓혀 다른 시설 아이콘과 무게를 맞춘다(세로형은 그대로).
                modifier = Modifier.size(width = 56.dp, height = 44.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(PixelIcons.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

private const val FACILITY_ASTRONAUT = "astronaut"
private const val FACILITY_HANGAR = "hangar"
private const val FACILITY_RESEARCH = "research"
private const val FACILITY_LOUNGE = "lounge"
