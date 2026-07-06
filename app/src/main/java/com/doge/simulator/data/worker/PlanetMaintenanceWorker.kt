package com.doge.simulator.data.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.doge.simulator.MainActivity
import com.doge.simulator.R
import com.doge.simulator.domain.model.GameConstants
import com.doge.simulator.domain.model.effectiveProduction
import com.doge.simulator.domain.repository.PlanetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class PlanetMaintenanceWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val planetRepository: PlanetRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val planets = planetRepository.getOwnedPlanets().first()
        if (planets.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        val capMs = GameConstants.MAX_OFFLINE_MINUTES * 60_000L
        val alertMs = (capMs * ALERT_FRACTION).toLong() // 20시간

        // 가장 오래 방치된 행성 기준으로 알림 여부 판단
        val earliestPlanet = planets.minByOrNull { it.lastProfitTime } ?: return Result.success()
        val elapsedMs = now - earliestPlanet.lastProfitTime

        if (elapsedMs < alertMs) return Result.success()

        // 이미 이 수집 사이클에서 알림을 보낸 경우 재발송 방지
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotifiedProfitTime = prefs.getLong(KEY_LAST_NOTIFIED_PROFIT_TIME, -1L)
        if (earliestPlanet.lastProfitTime == lastNotifiedProfitTime) return Result.success()

        val totalAccumulated = planets.sumOf { planet ->
            val elapsedMinutes = minOf(
                (now - planet.lastProfitTime) / 60_000L,
                GameConstants.MAX_OFFLINE_MINUTES
            )
            planet.effectiveProduction * elapsedMinutes
        }
        val remainingHours = ((capMs - elapsedMs) / 3_600_000L).coerceAtLeast(0L)

        sendNotification(planets.size, totalAccumulated, remainingHours)
        prefs.edit().putLong(KEY_LAST_NOTIFIED_PROFIT_TIME, earliestPlanet.lastProfitTime).apply()

        return Result.success()
    }

    private fun sendNotification(planetCount: Int, totalCoins: Long, remainingHours: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("deepLink", "doge://planet")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingText = if (remainingHours <= 0) "한도 도달!" else "${remainingHours}시간 후 한도"
        val bodyText = "${planetCount}개 행성 약 %,d 코인 누적 · $remainingText".format(totalCoins)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bottom_navi_planet_on)
            .setContentTitle("⚡ 행성 수익 알림")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_maintenance"
        const val WORK_NAME = "planet_maintenance_check"
        private const val NOTIFICATION_ID = 2001
        private const val REQUEST_CODE = 2001
        private const val PREFS_NAME = "planet_maintenance_prefs"
        private const val KEY_LAST_NOTIFIED_PROFIT_TIME = "last_notified_profit_time"
        private const val ALERT_FRACTION = 0.833 // 20/24 ≈ 83% → 20시간 경과 시 알림
    }
}
