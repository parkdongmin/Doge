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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doge.simulator.presentation.component.AppUpdateDialog
import com.doge.simulator.presentation.navigation.NavRoutes
import com.doge.simulator.presentation.screen.MainScreen
import com.doge.simulator.presentation.screen.SplashScreen
import com.doge.simulator.presentation.screen.auth.LoginScreen
import com.doge.simulator.ui.theme.DogeTheme
import com.doge.simulator.ui.theme.SpaceDark
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
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

    // ── 플레이 스토어 인앱 업데이트 ──────────────────────────────────
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    // 새 버전이 있으면 채워져 메인 화면에 안내 창이 뜬다
    private val availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    // 업데이트 화면에서 뒤로 나와도 게임은 그대로 이어서 한다 — 결과는 따로 처리하지 않음
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkForAppUpdate()

        // 알림 권한 요청 (Android 13+)
        requestNotificationPermissionIfNeeded()

        // 동의(UMP) 처리 후 광고 SDK 초기화
        requestConsentAndInitializeAds()

        // 최초 실행 시 딥링크 추출 (FCM 알림 탭 또는 URL scheme)
        _deepLink.value = extractDeepLink(intent)

        setContent {
            DogeTheme {
                val navController = rememberNavController()

                // 폰 기준으로 디자인된 화면을 폴더블 펼침 화면 등 넓은 기기에서 그대로
                // 가로로 늘리면 하단바·이미지가 비정상적으로 커진다. 최대 폭을 제한하고
                // 가운데 정렬 + 좌우 레터박스로 보여줘서 폰 비율을 유지한다.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SpaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 480.dp)
                    ) {
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
                                MainScreen(
                                    deepLinkFlow = deepLink,
                                    onSignOut = {
                                        navController.navigate(NavRoutes.Login.route) {
                                            popUpTo(NavRoutes.Main.route) { inclusive = true }
                                        }
                                    }
                                )
                                // 업데이트 안내는 게임에 들어온 뒤에만 — 시작·로그인 화면 위에는 띄우지 않는다
                                val update by availableUpdate.collectAsState()
                                update?.let { info ->
                                    AppUpdateDialog(
                                        onUpdate = {
                                            availableUpdate.value = null
                                            startImmediateUpdate(info)
                                        },
                                        onLater = {
                                            availableUpdate.value = null
                                            updatePromptDismissed = true
                                        }
                                    )
                                }
                            }
                        }
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

    // 플레이 스토어에 더 새 버전이 있는지 확인. 스토어 밖에서 설치한 빌드(디버그 등)나 확인 실패는 조용히 넘어간다
    private fun checkForAppUpdate() {
        if (updatePromptDismissed) return
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                availableUpdate.value = info
            }
        }
    }

    private fun startImmediateUpdate(info: AppUpdateInfo) {
        runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info, updateLauncher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            )
        }
    }

    // 업데이트 화면이 떠 있는 중에 앱을 벗어났다 돌아오면 구글 권장대로 업데이트 화면을 다시 띄운다
    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startImmediateUpdate(info)
            }
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

    companion object {
        // [나중에]를 누르면 이번 실행(프로세스) 동안은 다시 안 묻는다 — 화면 회전 등으로 액티비티가 다시 만들어져도 유지
        @Volatile private var updatePromptDismissed = false
    }
}