package com.doge.simulator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.doge.simulator.data.repository.CloudSaveManager
import com.doge.simulator.data.worker.PlanetEventWorker
import com.doge.simulator.data.worker.PlanetMaintenanceWorker
import com.doge.simulator.domain.usecase.SyncLeaderboardUseCase
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncLeaderboardUseCase: SyncLeaderboardUseCase

    @Inject
    lateinit var cloudSaveManager: CloudSaveManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        Coil.setImageLoader(newImageLoader())
        setupNotificationChannels()
        startLeaderboardSync()
        registerCloudSaveSync()
        schedulePlanetMaintenanceWorker()
        schedulePlanetEventWorker()
    }

    // 앱이 백그라운드로 갈 때(ON_STOP) 로컬 게임 상태를 클라우드로 백업.
    // "저장할 만한 순간"에만 push → 분당 write 없음 → 서버비 최소.
    private fun registerCloudSaveSync() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                appScope.launch { runCatching { cloudSaveManager.push() } }
            }
        })
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel("channel_exploration", "탐사 완료", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "탐사 완료 알림"
                    },
                    NotificationChannel("channel_training", "훈련 완료", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "우주인 훈련 완료 알림"
                    },
                    NotificationChannel(PlanetMaintenanceWorker.CHANNEL_ID, "행성 수익 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "행성 오프라인 수익 한도 임박 알림"
                    },
                    NotificationChannel(PlanetEventWorker.CHANNEL_ID, "행성 시황 속보", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "보유 행성에 큰 폭의 생산·시세 변동이 발생했을 때 알림"
                    }
                )
            )
        }
    }

    private fun schedulePlanetMaintenanceWorker() {
        val request = PeriodicWorkRequestBuilder<PlanetMaintenanceWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PlanetMaintenanceWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun schedulePlanetEventWorker() {
        val request = PeriodicWorkRequestBuilder<PlanetEventWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PlanetEventWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun startLeaderboardSync() {
        appScope.launch {
            while (true) {
                delay(30 * 60 * 1000L)
                runCatching { syncLeaderboardUseCase() }
                // 크래시로 ON_STOP push를 놓쳤을 때를 대비한 주기 백업
                runCatching { cloudSaveManager.push() }
            }
        }
    }
}
