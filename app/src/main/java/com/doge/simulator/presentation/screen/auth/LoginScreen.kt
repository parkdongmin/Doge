package com.doge.simulator.presentation.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.doge.simulator.util.findActivity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.BuildConfig
import com.doge.simulator.R
import com.doge.simulator.presentation.viewmodel.AuthViewModel
import com.doge.simulator.ui.theme.BrandBackgroundGradient
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceAccent
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.StatusRed
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.doge.simulator.presentation.component.PixelLoading

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by authViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signInInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    LaunchedEffect(state) {
        if (state is AuthViewModel.AuthState.Authenticated) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackgroundGradient)
    ) {
        // 하단 행성 일러스트 — 스플래시와 동일 위치·크기라 전환이 이어짐.
        // fillMaxWidth+FillWidth라 화면 폭 기준으로 커지지만, MainActivity에서
        // 앱 전체를 480dp 폭으로 제한해두어 폰 비율을 벗어나지 않는다.
        Image(
            painter = painterResource(R.drawable.splash_planet),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
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
                color = SpaceAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "행성을 탐험하고\n우주 최고의 투자자가 되어라",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 계정 선택·전체 화면 로그인이 진행되는 동안엔 버튼 대신 로딩 — 예전엔 버튼이 살아 있어
            // 연타하면 로그인 요청이 겹쳐 서로를 취소시킬 수 있었다
            val startSignIn = {
                if (!signInInProgress) {
                    signInInProgress = true
                    authViewModel.clearError()
                    launchGoogleSignIn(context.findActivity() as ComponentActivity, scope, authViewModel) {
                        signInInProgress = false
                    }
                }
            }
            when {
                state is AuthViewModel.AuthState.Loading || signInInProgress -> {
                    PixelLoading()
                }
                state is AuthViewModel.AuthState.Error -> {
                    Text(
                        text = (state as AuthViewModel.AuthState.Error).message,
                        color = StatusRed,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GoogleSignInButton(onClick = startSignIn)
                }
                else -> {
                    GoogleSignInButton(onClick = startSignIn)
                }
            }

            Spacer(modifier = Modifier.weight(0.68f))
        }
    }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SpaceNavy, RoundedCornerShape(4.dp))
            .border(1.dp, SpaceBlue, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Google로 시작하기",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

// 하단 시트 → (창 없이 실패하면) 전체 화면 순서로 시도한다 — 순서·판정은 GoogleSignInFlow 참고.
// onFinished는 어떤 경로로 끝나든(성공·취소·실패·화면 이탈) 정확히 한 번 불려 버튼을 다시 살린다
private fun launchGoogleSignIn(
    activity: ComponentActivity,
    scope: CoroutineScope,
    authViewModel: AuthViewModel,
    onFinished: () -> Unit
) {
    scope.launch {
        try {
            val credentialManager = CredentialManager.create(activity)
            val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            val flow = GoogleSignInFlow(
                uiMonitor = ActivityPauseMonitor(activity.lifecycle),
                clock = SystemClock::elapsedRealtime,
                bottomSheet = {
                    val option = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(clientId)
                        .build()
                    requestIdToken(credentialManager, activity, option)
                },
                fullScreen = {
                    requestIdToken(credentialManager, activity, GetSignInWithGoogleOption.Builder(clientId).build())
                }
            )
            when (val outcome = flow.run()) {
                is GoogleSignInFlow.Outcome.Token -> authViewModel.signInWithGoogle(outcome.idToken)
                GoogleSignInFlow.Outcome.UserCancelled -> Unit
                is GoogleSignInFlow.Outcome.Failed -> authViewModel.setError(
                    "Google 로그인 창을 열지 못했어요. 잠시 후 다시 시도해 주세요.\n오류 코드: ${outcome.code}"
                )
            }
        } finally {
            onFinished()
        }
    }
}

private suspend fun requestIdToken(
    credentialManager: CredentialManager,
    activity: ComponentActivity,
    option: CredentialOption
): String {
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = credentialManager.getCredential(activity, request).credential
    if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw GoogleSignInFlow.CredentialParseException("UNEXPECTED_CREDENTIAL")
    }
    return try {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } catch (e: GoogleIdTokenParsingException) {
        throw GoogleSignInFlow.CredentialParseException("TOKEN_PARSE")
    }
}

// 로그인 창(별도 화면)이 뜨면 우리 Activity가 ON_PAUSE를 받는다 — 시도 도중 그런 적이 있으면 "창이 떴다"
private class ActivityPauseMonitor(private val lifecycle: Lifecycle) : GoogleSignInFlow.UiShownMonitor {
    private var paused = false
    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE) paused = true
    }

    override fun begin() {
        paused = false
        lifecycle.addObserver(observer)
    }

    override fun end(): Boolean {
        lifecycle.removeObserver(observer)
        return paused
    }
}