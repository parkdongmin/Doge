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
import com.doge.simulator.domain.model.ExpeditionStatus
import com.doge.simulator.domain.repository.ExpeditionRepository
import com.doge.simulator.domain.usecase.CompleteExpeditionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ExplorationCompleteWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val expeditionRepository: ExpeditionRepository,
    private val completeExpeditionUseCase: CompleteExpeditionUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val expeditionId = inputData.getString(KEY_EXPEDITION_ID)

        // expeditionId가 있으면 해당 탐사만, 없으면 만료된 모든 탐사 처리
        val now = System.currentTimeMillis()
        val active = expeditionRepository.getActive().first()
        val toComplete = if (expeditionId != null) {
            active.filter { it.id == expeditionId && now >= it.endTime }
        } else {
            active.filter { it.status == ExpeditionStatus.IN_PROGRESS && now >= it.endTime }
        }

        toComplete.forEach { completeExpeditionUseCase(it) }

        if (toComplete.isNotEmpty()) sendExpeditionNotification(toComplete.size)

        return Result.success()
    }

    private fun sendExpeditionNotification(count: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("deepLink", "doge://explore")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_research_explore_skills)
            .setContentTitle("탐사 완료!")
            .setContentText(if (count == 1) "탐사가 완료되었습니다. 결과를 확인하세요!"
                            else "${count}개의 탐사가 완료되었습니다!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_exploration"
        const val NOTIFICATION_ID = 1001
        const val KEY_EXPEDITION_ID = "expedition_id"
    }
}
