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
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
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

    private val adsInitialized = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 알림 권한 요청 (Android 13+)
        requestNotificationPermissionIfNeeded()

        // 동의(UMP) 처리 후 광고 SDK 초기화
        requestConsentAndInitializeAds()

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
                            // 로그인 성공 후 Splash를 다시 거쳐 클라우드 세이브 복원을 태운다
                            // (새 기기 = 새 설치 + 로그인 시나리오가 이 경로).
                            onLoginSuccess = {
                                navController.navigate(NavRoutes.Splash.route) {
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

    // 딥링크를 읽은 뒤 intent에서 제거(consume)한다 — 그대로 두면 회전 등 설정 변경으로
    // Activity가 재생성될 때 onCreate가 같은 intent로 다시 호출되면서 딥링크 내비게이션이
    // 재실행돼, 이미 화면을 벗어난 사용자가 갑자기 예전 딥링크 화면으로 되돌아가게 된다
    private fun extractDeepLink(intent: Intent?): String? {
        // 1. doge:// scheme URL (intent-filter로 진입)
        val schemeUrl = intent?.data?.toString()
        if (!schemeUrl.isNullOrBlank()) {
            intent.data = null
            return schemeUrl
        }

        // 2. FCM data payload의 deepLink 필드
        val extra = intent?.getStringExtra("deepLink")
        if (!extra.isNullOrBlank()) {
            intent.removeExtra("deepLink")
            return extra
        }
        return null
    }

    /**
     * UMP(User Messaging Platform)로 동의 정보를 갱신하고, 필요 시 동의 폼을 띄운다.
     * 동의 처리가 끝나면(또는 지역상 불필요하다고 판단되면) 광고 SDK를 초기화한다.
     */
    private fun requestConsentAndInitializeAds() {
        val params = ConsentRequestParameters.Builder().build()
        val consentInformation: ConsentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    // formError 유무와 무관하게 이 시점엔 동의 처리가 끝난 상태이므로 광고 SDK 초기화 진행
                    initializeMobileAdsSdk()
                }
            },
            { initializeMobileAdsSdk() }
        )
        // 이미 동의가 확보돼 있어 폼이 필요 없는 경우, 콜백을 기다리지 않고 바로 초기화
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (adsInitialized.getAndSet(true)) return
        MobileAds.initialize(this) {}
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