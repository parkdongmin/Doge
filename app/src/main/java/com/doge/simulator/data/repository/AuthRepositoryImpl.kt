package com.doge.simulator.data.repository

import com.doge.simulator.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false
            saveUser(user, isNewUser)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() = auth.signOut()

    override suspend fun refreshSession() {
        val user = auth.currentUser ?: return
        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf("lastLoginAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
            .await()
    }

    // users/{uid}에는 리더보드에 필요한 것 + 민원대응용 타임스탬프만 저장한다.
    // email·photoUrl은 Firebase Auth가 이미 갖고 있어 중복이고(email은 규칙상 다른 유저에게
    // 읽혀 프라이버시 문제), fcmToken은 알림 전송 시점에 매번 새로 조회하므로 저장 불필요.
    private suspend fun saveUser(user: FirebaseUser, isNewUser: Boolean) {
        val data = mutableMapOf<String, Any?>(
            "uid" to user.uid,
            "displayName" to user.displayName,
            "lastLoginAt" to FieldValue.serverTimestamp()
        )
        if (isNewUser) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        firestore.collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }
}