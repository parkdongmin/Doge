package com.doge.simulator.domain.repository

import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    fun getCurrentUser(): FirebaseUser?
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    suspend fun signOut()
    /** 자동 로그인 시 lastLoginAt 갱신 (게임 데이터는 SyncLeaderboardUseCase가 처리) */
    suspend fun refreshSession()
}