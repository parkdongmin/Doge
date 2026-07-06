package com.doge.simulator.presentation.screen.auth

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.doge.simulator.BuildConfig
import com.doge.simulator.R
import com.doge.simulator.presentation.viewmodel.AuthViewModel
import com.doge.simulator.ui.theme.GoldAccent
import com.doge.simulator.ui.theme.SpaceAccent
import com.doge.simulator.ui.theme.SpaceBlue
import com.doge.simulator.ui.theme.SpaceDark
import com.doge.simulator.ui.theme.SpaceNavy
import com.doge.simulator.ui.theme.StatusRed
import com.doge.simulator.ui.theme.TextPrimary
import com.doge.simulator.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by authViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        authViewModel.checkAuthState()
    }

    LaunchedEffect(state) {
        if (state is AuthViewModel.AuthState.Authenticated) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
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
                color = SpaceAccent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "행성을 탐험하고\n우주 최고의 투자자가 되어라",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            when (state) {
                is AuthViewModel.AuthState.Loading -> {
                    CircularProgressIndicator(color = GoldAccent)
                }
                is AuthViewModel.AuthState.Error -> {
                    Text(
                        text = (state as AuthViewModel.AuthState.Error).message,
                        color = StatusRed,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GoogleSignInButton(
                        onClick = {
                            authViewModel.clearError()
                            launchGoogleSignIn(context as Activity, scope, authViewModel)
                        }
                    )
                }
                else -> {
                    GoogleSignInButton(
                        onClick = {
                            launchGoogleSignIn(context as Activity, scope, authViewModel)
                        }
                    )
                }
            }
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

private fun launchGoogleSignIn(
    activity: Activity,
    scope: kotlinx.coroutines.CoroutineScope,
    authViewModel: AuthViewModel
) {
    scope.launch {
        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            authViewModel.signInWithGoogle(googleCredential.idToken)
        } catch (e: GetCredentialException) {
            // GetCredentialCancellationException은 사용자가 직접 취소한 것이므로 무시
            val message = e.message ?: "Google 로그인을 사용할 수 없습니다"
            authViewModel.setError(message)
        }
    }
}