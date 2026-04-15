package com.doge.simulator.presentation.screen.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.presentation.viewmodel.ExploreViewModel
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var foundPlanet by remember { mutableStateOf<Planet?>(null) }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {

        Button(
            onClick = {
                scope.launch {
                    foundPlanet = viewModel.generatePlanet()
                }
            }
        ) {
            Text("탐험하기")
        }

        Spacer(modifier = Modifier.height(20.dp))

        foundPlanet?.let { planet ->

            Text("타입: ${planet.type}")
            Text("생산량: ${planet.production}")
            Text("위험도: ${planet.risk}")
            Text("투자: ${planet.investment}")
            Text("이벤트율: ${planet.eventRate}")
            Text("가격: ${planet.buyPrice}")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.buyPlanet(planet)
                }
            ) {
                Text("구매하기")
            }
        }
    }
}