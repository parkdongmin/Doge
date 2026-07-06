package com.doge.simulator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doge.simulator.presentation.navigation.NavRoutes
import com.doge.simulator.presentation.screen.MainScreen
import com.doge.simulator.presentation.screen.SplashScreen
import com.doge.simulator.presentation.screen.auth.LoginScreen
import com.doge.simulator.ui.theme.DogeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** FCM 딥링크 또는 Intent URL을 Compose 트리로 전달하는 StateFlow */
    private val _deepLink = MutableStateFlow<String?>(null)

    /** Android 13+ 알림 권한 런타임 요청 */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 허용 여부와 무관하게 별도 처리 없음 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 알림 권한 요청 (Android 13+)
        requestNotificationPermissionIfNeeded()

        // 최초 실행 시 딥링크 추출 (FCM 알림 탭 또는 URL scheme)
        _deepLink.value = extractDeepLink(intent)

        setContent {
            DogeTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Splash.route
                ) {
                    composable(NavRoutes.Splash.route) {
                        SplashScreen(
                            onNavigateToMain = {
                                navController.navigate(NavRoutes.Main.route) {
                                    popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate(NavRoutes.Login.route) {
                                    popUpTo(NavRoutes.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(NavRoutes.Login.route) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(NavRoutes.Main.route) {
                                    popUpTo(NavRoutes.Login.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(NavRoutes.Main.route) {
                        // StateFlow를 collectAsState()로 수집 → onNewIntent 시 자동 리컴포지션
                        val deepLink = _deepLink.asStateFlow()
                        MainScreen(deepLinkFlow = deepLink)
                    }
                }
            }
        }
    }

    /**
     * launchMode="singleTop" 설정 덕분에 앱이 이미 실행 중일 때
     * FCM 알림 탭 → 새 Activity 생성 없이 이 콜백으로 진입
     * setContent 재호출 없이 StateFlow 값만 업데이트
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _deepLink.value = extractDeepLink(intent)
    }

    private fun extractDeepLink(intent: Intent?): String? {
        // 1. doge:// scheme URL (intent-filter로 진입)
        val schemeUrl = intent?.data?.toString()
        if (!schemeUrl.isNullOrBlank()) return schemeUrl

        // 2. FCM data payload의 deepLink 필드
        return intent?.getStringExtra("deepLink")
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}