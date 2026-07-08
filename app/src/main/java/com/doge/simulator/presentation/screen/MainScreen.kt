package com.doge.simulator.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.doge.simulator.presentation.navigation.BottomNavItem
import com.doge.simulator.presentation.navigation.NavRoutes
import com.doge.simulator.presentation.screen.asset.AssetScreen
import com.doge.simulator.presentation.screen.explore.ExploreScreen
import com.doge.simulator.presentation.screen.expedition.ExpeditionHistoryScreen
import com.doge.simulator.presentation.screen.hq.AstronautScreen
import com.doge.simulator.presentation.screen.hq.HQScreen
import com.doge.simulator.presentation.screen.hq.HangarScreen
import com.doge.simulator.presentation.screen.hq.ResearchLabScreen
import com.doge.simulator.presentation.screen.planet.PlanetDetailScreen
import com.doge.simulator.presentation.screen.planet.PlanetScreen
import com.doge.simulator.presentation.screen.rank.RankScreen
import com.doge.simulator.ui.theme.*
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(deepLinkFlow: StateFlow<String?>) {
    val navController = rememberNavController()
    val deepLink by deepLinkFlow.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem.Explore,
        BottomNavItem.Planet,
        BottomNavItem.HQ,
        BottomNavItem.Asset
    )
    val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()
    var navBarHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(deepLink) {
        when {
            deepLink == null -> Unit
            deepLink!!.startsWith("doge://planet/") -> {
                val planetId = deepLink!!.removePrefix("doge://planet/")
                if (planetId.isNotBlank()) navController.navigate(NavRoutes.PlanetDetail.createRoute(planetId))
            }
            deepLink == "doge://explore" ->
                navController.navigate(NavRoutes.Explore.route) { launchSingleTop = true }
            deepLink == "doge://planet" ->
                navController.navigate(NavRoutes.Planet.route) { launchSingleTop = true }
            deepLink == "doge://hq" ->
                navController.navigate(NavRoutes.HQ.route) { launchSingleTop = true }
            deepLink == "doge://asset" ->
                navController.navigate(NavRoutes.Asset.route) { launchSingleTop = true }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 기본 배경색
        Box(modifier = Modifier.matchParentSize().background(SpaceDark))

        // 메인 콘텐츠
        // 하단 탭 바는 4개 탭 화면에서만 그려지므로, 그 외 서브 화면(우주인 센터·격납고·
        // 연구소 등)에서는 탭 바 높이만큼 빈 공간을 남기지 않도록 패딩을 조건부로 적용한다
        val bottomBarPadding = if (currentRoute in bottomNavRoutes)
            with(density) { navBarHeightPx.toDp() } else 0.dp
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Explore.route,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = bottomBarPadding)
            ) {
                // ── 하단 탭 4개 ───────────────────────────────────────
                composable(NavRoutes.Explore.route) {
                    ExploreScreen(
                        onNavigateToExpeditionHistory = {
                            navController.navigate(NavRoutes.ExpeditionHistory.route)
                        }
                    )
                }
                composable(NavRoutes.Planet.route) {
                    PlanetScreen(
                        onPlanetClick = { planetId ->
                            navController.navigate(NavRoutes.PlanetDetail.createRoute(planetId))
                        }
                    )
                }
                composable(NavRoutes.HQ.route) {
                    HQScreen(navController = navController)
                }
                composable(NavRoutes.Asset.route) {
                    AssetScreen(
                        onRankClick = { navController.navigate(NavRoutes.Rank.route) }
                    )
                }

                // ── 본부 서브 화면 ─────────────────────────────────────
                composable(NavRoutes.Astronaut.route) {
                    AstronautScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.Hangar.route) {
                    HangarScreen(onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.ResearchLab.route) {
                    ResearchLabScreen(onBack = { navController.popBackStack() })
                }

                // ── 행성 상세 ──────────────────────────────────────────
                composable(NavRoutes.PlanetDetail.route) { backStackEntry ->
                    val planetId = backStackEntry.arguments?.getString("planetId") ?: return@composable
                    PlanetDetailScreen(
                        planetId = planetId,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── 탐사 기록 ──────────────────────────────────────────
                composable(NavRoutes.ExpeditionHistory.route) {
                    ExpeditionHistoryScreen(onBack = { navController.popBackStack() })
                }

                // ── 랭킹 (자산 탭에서 진입) ────────────────────────────
                composable(NavRoutes.Rank.route) {
                    RankScreen(onHomeClick = { navController.popBackStack() })
                }
        }

        // 바텀 네비게이션 — bg_main 위에 직접 올려서 배경 없이 렌더링
        if (currentRoute in bottomNavRoutes) {
            PixelBottomNavBar(
                navController = navController,
                items = bottomNavItems,
                currentRoute = currentRoute,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { navBarHeightPx = it.height }
            )
        }
    }
}

@Composable
private fun PixelBottomNavBar(
    navController: NavController,
    items: List<BottomNavItem>,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SpaceDark)
            .navigationBarsPadding()
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            Image(
                painter = painterResource(if (selected) item.iconOn else item.iconOff),
                contentDescription = item.title,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(90f / 68f)
                    .clickable {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
            )
        }
    }
}
