package com.doge.simulator.presentation.screen.planet

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.presentation.viewmodel.PlanetViewModel

@Composable
fun PlanetScreen(
    viewModel: PlanetViewModel = hiltViewModel()
) {
    val planets by viewModel.planets.collectAsState()

    LazyColumn {
        items(planets) { planet ->
            Text("${planet.type} - ${planet.currentValue}")
        }
    }
}