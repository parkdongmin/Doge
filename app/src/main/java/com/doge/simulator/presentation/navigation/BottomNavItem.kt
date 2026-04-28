package com.doge.simulator.presentation.navigation

import androidx.annotation.DrawableRes
import com.doge.simulator.R

sealed class BottomNavItem(
    val title: String,
    val route: String,
    @DrawableRes val icon: Int
) {
    object Explore : BottomNavItem("탐험", NavRoutes.Explore.route, R.drawable.ic_bottom_navi_explore)
    object Planet : BottomNavItem("행성", NavRoutes.Planet.route, R.drawable.ic_bottom_navi_planet)
    object Feed : BottomNavItem("피드", NavRoutes.Feed.route, R.drawable.ic_bottom_navi_feed)
    object Asset : BottomNavItem("자산", NavRoutes.Asset.route, R.drawable.ic_bottom_navi_asset)
}