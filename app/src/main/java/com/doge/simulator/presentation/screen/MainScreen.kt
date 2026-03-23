package com.doge.simulator.presentation.screen

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.doge.simulator.presentation.navigation.*
import com.doge.simulator.presentation.screen.asset.AssetScreen
import com.doge.simulator.presentation.screen.explore.ExploreScreen
import com.doge.simulator.presentation.screen.feed.FeedScreen
import com.doge.simulator.presentation.screen.planet.PlanetScreen

@Composable
fun MainScreen() {

    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Explore.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.Explore.route) { ExploreScreen() }
            composable(NavRoutes.Planet.route) { PlanetScreen() }
            composable(NavRoutes.Feed.route) { FeedScreen() }
            composable(NavRoutes.Asset.route) { AssetScreen() }
        }
    }
}