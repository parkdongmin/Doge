package com.doge.simulator.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.ui.graphics.vector.ImageVector
import com.doge.simulator.R

sealed class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    @DrawableRes val iconOn: Int,
    @DrawableRes val iconOff: Int
) {
    object Explore : BottomNavItem(
        "탐사", NavRoutes.Explore.route,
        Icons.Default.RocketLaunch,
        R.drawable.ic_bottom_navi_explore_on,
        R.drawable.ic_bottom_navi_explore_off
    )
    object Planet : BottomNavItem(
        "행성", NavRoutes.Planet.route,
        Icons.Default.Public,
        R.drawable.ic_bottom_navi_planet_on,
        R.drawable.ic_bottom_navi_planet_off
    )
    object HQ : BottomNavItem(
        "정거장", NavRoutes.HQ.route,
        Icons.Default.Home,
        R.drawable.ic_bottom_navi_space_station_on,
        R.drawable.ic_bottom_navi_space_station_off
    )
    object Asset : BottomNavItem(
        "자산", NavRoutes.Asset.route,
        Icons.Default.AccountBalanceWallet,
        R.drawable.ic_bottom_navi_asset_on,
        R.drawable.ic_bottom_navi_asset_off
    )
}
