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
import com.doge.simulator.domain.repository.PlanetRepository
import com.doge.simulator.domain.usecase.PlanetEventRoll
import com.doge.simulator.domain.usecase.RollPlanetEventUseCase
import com.doge.simulator.util.withSubjectParticle
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

// 앱을 안 열고 있어도 밀린 행성 이벤트를 주기적으로 굴려서, 그중 "큰 폭"으로 움직인 것만
// 골라 알림을 보낸다 — 주식 앱이 1%는 조용히 넘기고 20% 급등락에만 알림을 보내는 것과 같은
// 개념. 이벤트 롤 자체는 CollectProfitUseCase(포그라운드 진입 시)와 같은 로직을
// RollPlanetEventUseCase로 공유하므로, 이 워커가 먼저 굴리든 나중에 앱을 열어서 굴리든
// 결과가 달라지지 않는다(lastEventTime 기준으로 한 번만 롤됨)
@HiltWorker
class PlanetEventWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val planetRepository: PlanetRepository,
    private val rollPlanetEventUseCase: RollPlanetEventUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        doWorkInner()
    } catch (e: Exception) {
        // 일시적 실패는 재시도 — 안 그러면 이 워커가 담당하는 알림만 조용히 영영 안 뜨게 된다
        Result.retry()
    }

    private suspend fun doWorkInner(): Result {
        val planets = planetRepository.getOwnedPlanets().first()
        if (planets.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        val bigMoves = planets.mapNotNull { planet ->
            rollPlanetEventUseCase(planet, now)
                ?.takeIf { it.magnitude >= GameConstants.PLANET_EVENT_NOTIFY_DELTA_THRESHOLD }
        }

        if (bigMoves.isNotEmpty()) sendNotification(bigMoves)

        return Result.success()
    }

    private fun sendNotification(bigMoves: List<PlanetEventRoll>) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("deepLink", "doge://planet")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (bigMoves.size == 1) {
            val move = bigMoves[0]
            val verb = if (move.isBad) "폭락" else "폭등"
            "${move.planetDisplayName.withSubjectParticle()} ${verb}했습니다!"
        } else {
            val badCount = bigMoves.count { it.isBad }
            val goodCount = bigMoves.size - badCount
            "${bigMoves.size}개 행성에 큰 변동 발생 (폭등 $goodCount · 폭락 $badCount)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ui_planet)
            .setContentTitle("행성 시황 속보")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_planet_event"
        const val WORK_NAME = "planet_event_check"
        private const val NOTIFICATION_ID = 3001
        private const val REQUEST_CODE = 3001
    }
}
