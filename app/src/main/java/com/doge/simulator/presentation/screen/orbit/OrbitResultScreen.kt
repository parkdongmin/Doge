package com.doge.simulator.presentation.screen.orbit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.domain.model.orbit.MatchOutcome
import com.doge.simulator.presentation.viewmodel.OrbitViewModel
import com.doge.simulator.ui.theme.*

@Composable
fun OrbitResultScreen(
    onPlayAgain: () -> Unit,
    onReturnToLounge: () -> Unit,
    viewModel: OrbitViewModel = hiltViewModel()
) {
    val settlement by viewModel.lastSettlement.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
            .statusBarsPadding()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val won = settlement?.outcome == MatchOutcome.WON
        Text(
            if (won) "YOU WIN!" else "YOU LOSE...",
            color = if (won) StatusGreen else StatusRed,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.lg))
        settlement?.let {
            val label = if (won) "WIN +${it.netChange}" else "RESULT ${it.netChange}"
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.xxl))
        Button(
            onClick = { viewModel.playAgain(); onPlayAgain() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("다시 하기") }
        Spacer(Modifier.height(Spacing.sm))
        OutlinedButton(
            onClick = { viewModel.returnToLounge(); onReturnToLounge() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("휴게실로") }
    }
}
