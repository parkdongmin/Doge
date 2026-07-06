package com.doge.simulator.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationFirestoreService @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sendNotification(
        type: String,
        title: String,
        body: String,
        deepLink: String = "",
        channelId: String = "channel_market"
    ) {
        val token = FirebaseMessaging.getInstance().token.await()
        val data = hashMapOf(
            "type" to type,
            "title" to title,
            "body" to body,
            "deepLink" to deepLink,
            "channelId" to channelId,
            "createdAt" to System.currentTimeMillis()
        )
        firestore
            .collection("notifications")
            .document(token)
            .collection("pending")
            .add(data)
            .await()
    }
}
