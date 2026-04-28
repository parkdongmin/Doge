package com.doge.simulator.presentation.screen.planet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.Planet
import com.doge.simulator.presentation.viewmodel.PlanetViewModel

@Composable
fun PlanetScreen(
    viewModel: PlanetViewModel = hiltViewModel(),
    onPlanetClick: (Planet) -> Unit = {}
) {
    val planets by viewModel.planets.collectAsState()

    LazyColumn {
        items(planets) { planet ->
            PlanetCard(
                planet = planet,
                onClick = { onPlanetClick(planet) }
            )
        }
    }
}

@Composable
fun PlanetCard(
    planet: Planet,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1A3A) // 배경과 어울리는 딥블루톤
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🪐 ${planet.type}",
                color = Color(0xFFFEDC56),   // 노란 포인트 색
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("가치: ${planet.currentValue}", color = Color.White)
            Text("레벨: ${planet.level}", color = Color.White)
            Text("총 수익: ${planet.totalProfit}", color = Color.White)
        }
    }
}