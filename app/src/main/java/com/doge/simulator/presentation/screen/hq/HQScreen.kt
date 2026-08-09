package com.doge.simulator.presentation.screen.hq

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.doge.simulator.ui.theme.*

@Composable
fun HQScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
    ) {
        // ── 제목 ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.lg, bottom = Spacing.md)
        ) {
            Text(
                text = "정거장",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "시설을 관리하여 탐사 역량을 강화하세요",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // ── 정거장 이미지 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
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
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
        ) {
            HQFacilityCard(
                iconRes = R.drawable.character_1,
                title = "우주인 센터",
                description = "우주인 고용 및 훈련 관리",
                onClick = { navController.navigate(NavRoutes.Astronaut.route) }
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            HQFacilityCard(
                iconRes = R.drawable.spaceship_2,
                title = "격납고",
                description = "우주선 구매 및 강화",
                onClick = { navController.navigate(NavRoutes.Hangar.route) }
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            HQFacilityCard(
                iconRes = R.drawable.ic_space_station_research,
                title = "연구소",
                description = "탐사 기술·천체 분석·인사·공학 연구",
                onClick = { navController.navigate(NavRoutes.ResearchLab.route) }
            )
        }
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
                modifier = Modifier.size(44.dp)
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
            Text(text = ">", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
