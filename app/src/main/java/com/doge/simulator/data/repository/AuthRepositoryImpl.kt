package com.doge.simulator.data.repository

import com.doge.simulator.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) : AuthRepository {

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false
            val fcmToken = runCatching { messaging.token.await() }.getOrNull() ?: ""
            saveUser(user, fcmToken, isNewUser)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() = auth.signOut()

    override suspend fun refreshSession() {
        val user = auth.currentUser ?: return
        val fcmToken = runCatching { messaging.token.await() }.getOrNull() ?: return
        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "fcmToken"    to fcmToken,
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private suspend fun saveUser(user: FirebaseUser, fcmToken: String, isNewUser: Boolean) {
        val data = mutableMapOf<String, Any?>(
            "uid" to user.uid,
            "email" to user.email,
            "displayName" to user.displayName,
            "photoUrl" to user.photoUrl?.toString(),
            "fcmToken" to fcmToken,
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