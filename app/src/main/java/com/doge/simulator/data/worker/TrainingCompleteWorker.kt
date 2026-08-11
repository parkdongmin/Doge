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
import com.doge.simulator.domain.model.AstronautStatus
import com.doge.simulator.domain.repository.AstronautRepository
import com.doge.simulator.domain.usecase.CompleteTrainingUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class TrainingCompleteWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val astronautRepository: AstronautRepository,
    private val completeTrainingUseCase: CompleteTrainingUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val astronautId = inputData.getString(KEY_ASTRONAUT_ID)
        val now = System.currentTimeMillis()

        val completed = astronautRepository.getAstronauts().first()
            .filter { astronaut ->
                val matchesId = astronautId == null || astronaut.id == astronautId
                val isTraining = astronaut.status == AstronautStatus.TRAINING
                val isDone = (astronaut.trainingEndTime ?: Long.MAX_VALUE) <= now
                matchesId && isTraining && isDone
            }

        completed.forEach { astronaut ->
            completeTrainingUseCase(astronaut)
            sendTrainingNotification(astronaut.name)
        }

        Result.success()
    } catch (e: Exception) {
        // 일시적 실패는 재시도 — 포그라운드 폴링이 게임 상태는 self-heal하지만, 재시도가
        // 없으면 이 워커가 담당하는 완료 알림만 조용히 영영 안 뜨게 된다
        Result.retry()
    }

    private fun sendTrainingNotification(name: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("deepLink", "doge://hq")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, name.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_space_station_research)
            .setContentTitle("훈련 완료!")
            .setContentText("$name 의 훈련이 완료되었습니다!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID_BASE + name.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_training"
        const val NOTIFICATION_ID_BASE = 2000
        const val KEY_ASTRONAUT_ID = "astronaut_id"
    }
}
