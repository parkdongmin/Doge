package com.doge.simulator.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.presentation.viewmodel.AuthViewModel
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceAccent
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.SpaceLight
import com.doge.simulator.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        delay(1500)
        authViewModel.checkAuthState()
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Authenticated -> onNavigateToMain()
            is AuthViewModel.AuthState.Unauthenticated -> onNavigateToLogin()
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DOGE",
                style = MaterialTheme.typography.displayLarge,
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PLANET SIMULATOR",
                style = MaterialTheme.typography.labelMedium,
                color = SpaceLight
            )
            Spacer(modifier = Modifier.height(48.dp))
            PixelLoadingDots()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun PixelLoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dotCount = 3

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(dotCount) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis = index * 150,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(SpaceAccent)
            )
        }
    }
}