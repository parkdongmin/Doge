package com.doge.simulator.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.doge.simulator.presentation.navigation.*
import com.doge.simulator.presentation.screen.asset.AssetScreen
import com.doge.simulator.presentation.screen.explore.ExploreScreen
import com.doge.simulator.presentation.screen.feed.FeedScreen
import com.doge.simulator.presentation.screen.planet.PlanetScreen
import com.doge.simulator.R

@Composable
fun MainScreen() {

    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.bg_main),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 화면
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Explore.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NavRoutes.Explore.route) { ExploreScreen() }
            composable(NavRoutes.Planet.route) { PlanetScreen() }
            composable(NavRoutes.Feed.route) { FeedScreen() }
            composable(NavRoutes.Asset.route) { AssetScreen() }
        }

        BottomImageNavigationBar(
            currentRoute = currentRoute,
            onClick = { route -> navController.navigate(route) }
        )
    }
}

@Composable
fun BottomImageNavigationBar(
    currentRoute: String?,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 68.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        ImageNavButton(
            imageRes = if (currentRoute == NavRoutes.Explore.route)
                R.drawable.ic_bottom_navi_explore
            else R.drawable.ic_bottom_navi_explore,
            onClick = { onClick(NavRoutes.Explore.route) }
        )

        ImageNavButton(
            imageRes = if (currentRoute == NavRoutes.Planet.route)
                R.drawable.ic_bottom_navi_planet
            else R.drawable.ic_bottom_navi_planet,
            onClick = { onClick(NavRoutes.Planet.route) }
        )

        ImageNavButton(
            imageRes = if (currentRoute == NavRoutes.Feed.route)
                R.drawable.ic_bottom_navi_feed
            else R.drawable.ic_bottom_navi_feed,
            onClick = { onClick(NavRoutes.Feed.route) }
        )

        ImageNavButton(
            imageRes = if (currentRoute == NavRoutes.Asset.route)
                R.drawable.ic_bottom_navi_asset
            else R.drawable.ic_bottom_navi_asset,
            onClick = { onClick(NavRoutes.Asset.route) }
        )
    }
}

@Composable
fun ImageNavButton(
    imageRes: Int,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(imageRes),
        contentDescription = null,
        modifier = Modifier
            .size(84.dp)
            .clickable(onClick = onClick)
    )
}