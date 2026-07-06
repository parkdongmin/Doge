package com.doge.simulator.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doge.simulator.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun checkAuthState() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            _state.value = AuthState.Authenticated(user)
            // 자동 로그인: fcmToken·lastLoginAt을 백그라운드에서 조용히 갱신
            viewModelScope.launch {
                runCatching { authRepository.refreshSession() }
            }
        } else {
            _state.value = AuthState.Unauthenticated
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user -> _state.value = AuthState.Authenticated(user) }
                .onFailure { e -> _state.value = AuthState.Error(e.message ?: "로그인 실패") }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }

    fun setError(message: String) {
        _state.value = AuthState.Error(message)
    }
}