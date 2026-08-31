package com.doge.simulator.presentation.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.R
import com.doge.simulator.presentation.viewmodel.AuthViewModel
import com.doge.simulator.presentation.viewmodel.BootstrapViewModel
import com.doge.simulator.ui.theme.BrandBackgroundGradient
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceAccent
import com.doge.simulator.ui.theme.SpaceLight
import com.doge.simulator.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    bootstrapViewModel: BootstrapViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val bootstrapPhase by bootstrapViewModel.phase.collectAsState()

    LaunchedEffect(Unit) {
        delay(1500)
        authViewModel.checkAuthState()
    }

    LaunchedEffect(authState) {
        when (authState) {
            // 로그인 확인됨 → 클라우드 세이브 복원 후 진입 (아래 bootstrapPhase 관찰에서 내비게이션)
            is AuthViewModel.AuthState.Authenticated -> bootstrapViewModel.restoreOnce()
            is AuthViewModel.AuthState.Unauthenticated -> onNavigateToLogin()
            else -> Unit
        }
    }

    LaunchedEffect(bootstrapPhase) {
        if (bootstrapPhase == BootstrapViewModel.Phase.DONE) onNavigateToMain()
    }

    SplashContent(isSyncing = bootstrapPhase == BootstrapViewModel.Phase.RESTORING)
}

@Composable
private fun SplashContent(isSyncing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackgroundGradient)
    ) {
        // 하단 행성 일러스트
        Image(
            painter = painterResource(R.drawable.splash_planet),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        // 로고 + 로딩 (화면 상단 1/3 지점)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                text = "Doge",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 46.sp,
                    lineHeight = 54.sp
                ),
                color = GoldAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "PLANET SIMULATOR",
                style = MaterialTheme.typography.labelMedium,
                color = SpaceLight
            )

            Spacer(modifier = Modifier.height(44.dp))
            PixelLoadingDots()
            if (isSyncing) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "동기화 중...",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(0.68f))
        }

        // 버전
        Text(
            text = "v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        )
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
