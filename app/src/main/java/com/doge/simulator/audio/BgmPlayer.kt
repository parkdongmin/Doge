package com.doge.simulator.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.MainThread
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.doge.simulator.data.local.SettingsPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

// 앱 전역 배경 음악. 특정 화면이 아니라 프로세스 라이프사이클에 묶인다 — 앱이 포그라운드일
// 때만 재생하고, 백그라운드로 가면 멈춘다(다시 켜지면 이어서 재생). 설정 토글
// ([[SettingsPrefs]].bgmEnabled)에 즉시 반응하고, 다른 앱이 오디오를 잡으면(전화·유튜브 등)
// 포커스를 양보한다.
//
// 음원 파일(res/raw/bgm_ambient.mp3)은 라이선스 정리 전까지 레포에 안 올라가 있어(.gitignore)
// 리소스를 이름으로 조회한다 — 파일이 없으면 id가 0이라 조용히 아무것도 안 한다.
@Singleton
class BgmPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsPrefs: SettingsPrefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var player: MediaPlayer? = null
    private var started = false

    private var enabled = settingsPrefs.bgmEnabled.value
    private var appInForeground = false
    private var hasAudioFocus = false

    private val trackResId: Int
        get() = context.resources.getIdentifier("bgm_ambient", "raw", context.packageName)

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                player?.setVolume(FULL_VOLUME, FULL_VOLUME)
                sync()
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                sync()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                player?.setVolume(DUCK_VOLUME, DUCK_VOLUME)
        }
    }

    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(focusListener)
                .build()
        } else null

    /** MyApplication.onCreate에서 한 번 호출 (메인 스레드). */
    @MainThread
    fun start() {
        if (started) return
        started = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appInForeground = true
                sync()
            }

            override fun onStop(owner: LifecycleOwner) {
                appInForeground = false
                sync()
            }
        })
        settingsPrefs.bgmEnabled
            .onEach { enabled = it; sync() }
            .launchIn(scope)
    }

    private fun sync() {
        val shouldPlay = enabled && appInForeground && trackResId != 0
        if (shouldPlay) {
            if (!hasAudioFocus) requestFocus()
            if (!hasAudioFocus) return
            val mp = ensurePlayer() ?: return
            if (!mp.isPlaying) mp.start()
        } else {
            player?.let { if (it.isPlaying) it.pause() }
            // 사용자가 껐거나 앱이 백그라운드면 포커스를 다른 앱에 넘긴다.
            // (일시적 포커스 상실은 그대로 두고 복귀를 기다린다)
            if (!enabled || !appInForeground) abandonFocus()
        }
    }

    private fun ensurePlayer(): MediaPlayer? {
        player?.let { return it }
        val resId = trackResId
        if (resId == 0) return null
        return try {
            MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(FULL_VOLUME, FULL_VOLUME)
            }.also { player = it }
        } catch (e: Exception) {
            null
        }
    }

    private fun requestFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.requestAudioFocus(it) }
                ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        hasAudioFocus = false
    }

    private companion object {
        const val FULL_VOLUME = 1f
        const val DUCK_VOLUME = 0.2f
    }
}
