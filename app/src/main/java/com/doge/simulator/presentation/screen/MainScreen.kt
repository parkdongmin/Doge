package com.doge.simulator.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.doge.simulator.presentation.component.OfflineLossDialog
import com.doge.simulator.presentation.component.OfflineProfitDialog
import com.doge.simulator.presentation.navigation.BottomNavItem
import com.doge.simulator.presentation.navigation.NavRoutes
import com.doge.simulator.presentation.screen.asset.AssetScreen
import com.doge.simulator.presentation.screen.explore.ExploreScreen
import com.doge.simulator.presentation.screen.expedition.ExpeditionLogPanel
import com.doge.simulator.presentation.screen.hq.HQScreen
import com.doge.simulator.presentation.screen.orbit.OrbitGameScreen
import com.doge.simulator.presentation.screen.orbit.OrbitResultScreen
import com.doge.simulator.presentation.screen.planet.PlanetDetailScreen
import com.doge.simulator.presentation.screen.planet.PlanetScreen
import com.doge.simulator.presentation.screen.rank.RankScreen
import com.doge.simulator.presentation.tutorial.LocalTutorialTargets
import com.doge.simulator.presentation.tutorial.TutorialOverlay
import com.doge.simulator.presentation.tutorial.TutorialStep
import com.doge.simulator.presentation.tutorial.TutorialTargetRegistry
import com.doge.simulator.presentation.tutorial.TutorialViewModel
import com.doge.simulator.presentation.viewmodel.AppSessionViewModel
import com.doge.simulator.ui.theme.*
import com.doge.simulator.util.findActivity
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(deepLinkFlow: StateFlow<String?>, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val deepLink by deepLinkFlow.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appSessionViewModel: AppSessionViewModel = hiltViewModel()
    val pendingOfflineProfit by appSessionViewModel.pendingOfflineProfit.collectAsState()
    val offlineAdNotice by appSessionViewModel.offlineAdNotice.collectAsState()

    val tutorialViewModel: TutorialViewModel = hiltViewModel()
    val tutorialStep by tutorialViewModel.step.collectAsState()
    val tutorialTargets = remember { TutorialTargetRegistry() }
    LaunchedEffect(currentRoute) { tutorialViewModel.onRouteChanged(currentRoute) }
    val activity = LocalContext.current.findActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) appSessionViewModel.checkPendingProfit()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Explore,
        BottomNavItem.Planet,
        BottomNavItem.HQ,
        BottomNavItem.Asset
    )
    val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()
    var navBarHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // 탭 딥링크는 하단 탭 바 전환과 동일하게 popUpTo·restoreState를 적용해, 새 탭 인스턴스가
    // 백스택에 계속 쌓이는 것을 막는다 (그대로 두면 알림을 여러 번 탭할 때마다 중복된 탭
    // 화면이 쌓여 뒤로가기가 한 번에 앱을 종료하지 못하고 중복 화면을 거쳐가게 된다)
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(deepLink) {
        when {
            deepLink == null -> Unit
            deepLink!!.startsWith("doge://planet/") -> {
                val planetId = deepLink!!.removePrefix("doge://planet/")
                if (planetId.isNotBlank()) {
                    navController.navigate(NavRoutes.PlanetDetail.createRoute(planetId)) {
                        launchSingleTop = true
                    }
                }
            }
            deepLink == "doge://explore" -> navigateToTab(NavRoutes.Explore.route)
            deepLink == "doge://planet" -> navigateToTab(NavRoutes.Planet.route)
            deepLink == "doge://hq" -> navigateToTab(NavRoutes.HQ.route)
            deepLink == "doge://asset" -> navigateToTab(NavRoutes.Asset.route)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      CompositionLocalProvider(LocalTutorialTargets provides tutorialTargets) {
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
                    // 탐사 일지는 새 화면 대신 탐험 탭 위에 창으로 연다(FacilityPanel 참고).
                    var expeditionLogOpen by rememberSaveable { mutableStateOf(false) }
                    Box(Modifier.fillMaxSize()) {
                        ExploreScreen(onNavigateToExpeditionHistory = { expeditionLogOpen = true })
                        ExpeditionLogPanel(visible = expeditionLogOpen, onClose = { expeditionLogOpen = false })
                    }
                }
                composable(NavRoutes.Planet.route) {
                    PlanetScreen(
                        onPlanetClick = { planetId ->
                            navController.navigate(NavRoutes.PlanetDetail.createRoute(planetId)) { launchSingleTop = true }
                        }
                    )
                }
                composable(NavRoutes.HQ.route) { backStackEntry ->
                    HQScreen(
                        navController = navController,
                        orbitViewModel = hiltViewModel(backStackEntry)
                    )
                }
                composable(NavRoutes.Asset.route) {
                    AssetScreen(
                        onRankClick = { navController.navigate(NavRoutes.Rank.route) { launchSingleTop = true } },
                        onSignedOut = onSignOut
                    )
                }

                // ── 행성 상세 ──────────────────────────────────────────
                composable(NavRoutes.PlanetDetail.route) { backStackEntry ->
                    val planetId = backStackEntry.arguments?.getString("planetId") ?: return@composable
                    PlanetDetailScreen(
                        planetId = planetId,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── 랭킹 (자산 탭에서 진입) ────────────────────────────
                composable(NavRoutes.Rank.route) {
                    RankScreen(onHomeClick = { navController.popBackStack() })
                }

                // ── ORBIT 카드게임(휴게실) ─────────────────────────────
                // 휴게실은 정거장(HQ) 위에 뜨는 창(LoungePanel)이고, 게임/결과는 별도 전체 화면이다.
                // 셋이 매치 진행 중 같은 OrbitViewModel(매치 상태)을 공유해야 하므로, 각 화면의 기본
                // hiltViewModel()(현재 백스택 엔트리 스코프) 대신 항상 HQ 엔트리에 스코프된 하나의
                // 인스턴스를 명시적으로 넘겨준다. 이게 없으면 화면을 옮길 때마다 새 ViewModel이 생겨
                // 진행 중이던 매치 상태가 사라진다. 게임·결과에서 HQ로 돌아오면 휴게실 창이 열린 채
                // 그대로 있다(HQScreen의 openFacility는 rememberSaveable).
                composable(NavRoutes.OrbitGame.route) {
                    val hqEntry = remember(navController) {
                        navController.getBackStackEntry(NavRoutes.HQ.route)
                    }
                    OrbitGameScreen(
                        onExit = { navController.popBackStack(NavRoutes.HQ.route, false) },
                        onMatchFinished = {
                            navController.navigate(NavRoutes.OrbitResult.route) {
                                popUpTo(NavRoutes.HQ.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        viewModel = hiltViewModel(hqEntry)
                    )
                }
                composable(NavRoutes.OrbitResult.route) {
                    val hqEntry = remember(navController) {
                        navController.getBackStackEntry(NavRoutes.HQ.route)
                    }
                    OrbitResultScreen(
                        // 휴게실로 돌아가면 베팅 모달이 바로 열려 있다(viewModel.playAgain()이 연다).
                        onPlayAgain = { navController.popBackStack(NavRoutes.HQ.route, false) },
                        onReturnToLounge = { navController.popBackStack(NavRoutes.HQ.route, false) },
                        viewModel = hiltViewModel(hqEntry)
                    )
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

        // ── 튜토리얼 오버레이 ─────────────────────────────────────────
        // 화면별 표시 조건은 TutorialViewModel이 라우트를 받아 판정한다.
        // 오프라인 수익/손실 다이얼로그가 떠 있으면 양보.
        if (pendingOfflineProfit == null && tutorialStep !is TutorialStep.None) {
            TutorialOverlay(
                step = tutorialStep,
                targets = tutorialTargets,
                onAdvancePart1 = { tutorialViewModel.advancePart1() },
                onDismiss = { tutorialViewModel.dismiss(it) },
            )
        }
      }
    }

    // ── 오프라인 수익/손실 확인 다이얼로그 (탭과 무관하게 최상단에 노출) ──
    pendingOfflineProfit?.let { pending ->
        if (pending.coins >= 0) {
            OfflineProfitDialog(
                coins = pending.coins,
                adNotice = offlineAdNotice,
                onClaimWithAd = { appSessionViewModel.claimWithAd(activity) },
                onClaimFree = { appSessionViewModel.claimFree() }
            )
        } else {
            OfflineLossDialog(
                coins = -pending.coins,
                onAcknowledge = { appSessionViewModel.claimFree() }
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
