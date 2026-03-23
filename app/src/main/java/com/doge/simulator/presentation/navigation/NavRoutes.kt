package com.doge.simulator.presentation.navigation

sealed class NavRoutes(val route: String) {
    object Explore : NavRoutes("explore")
    object Planet : NavRoutes("planet")
    object Feed : NavRoutes("feed")
    object Asset : NavRoutes("asset")
}