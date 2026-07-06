package com.doge.simulator.firebase

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.doge.simulator.MainActivity
import com.doge.simulator.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class DogeFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * 앱이 포그라운드일 때 FCM 메시지 수신 → 로컬 알림 직접 표시
     * 백그라운드/종료 상태에서는 FCM SDK가 자동으로 알림 표시
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: return
        val body = remoteMessage.notification?.body ?: return
        val deepLink = remoteMessage.data["deepLink"] ?: ""
        val channelId = remoteMessage.notification?.channelId ?: "channel_market"

        showLocalNotification(title, body, deepLink, channelId)
    }

    /**
     * FCM 토큰 갱신 이벤트
     * NotificationFirestoreService가 매 전송 시점에 최신 토큰을 조회하므로
     * 별도 저장 처리는 불필요
     */
    override fun onNewToken(token: String) {
        // no-op: 토큰은 sendNotification 시 실시간 조회
    }

    private fun showLocalNotification(
        title: String,
        body: String,
        deepLink: String,
        channelId: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLink.isNotBlank()) putExtra("deepLink", deepLink)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Random.nextInt(), notification)
    }
}