package com.doge.simulator.presentation.navigation

sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Login : NavRoutes("login")
    object Main : NavRoutes("main")

    // ── 하단 탭 4개 ───────────────────────────────────────────────────
    object Explore : NavRoutes("explore")
    object Planet : NavRoutes("planet")
    object HQ : NavRoutes("hq")
    object Asset : NavRoutes("asset")

    // ── 서브 화면 ─────────────────────────────────────────────────────
    object PlanetDetail : NavRoutes("planet_detail/{planetId}") {
        fun createRoute(planetId: String) = "planet_detail/$planetId"
    }
    object Astronaut : NavRoutes("astronaut")
    object Hangar : NavRoutes("hangar")
    object ResearchLab : NavRoutes("research_lab")
    object ExpeditionHistory : NavRoutes("expedition_history")
    object Rank : NavRoutes("rank")
}
