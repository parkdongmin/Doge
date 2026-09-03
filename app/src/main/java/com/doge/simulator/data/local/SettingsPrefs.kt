package com.doge.simulator.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// 앱 환경설정(게임 진행상태 아님). 기기 로컬이라 prefs로 충분하고 클라우드 세이브에도 안 실린다
// ([[project_cloud_save]] 스냅샷 제외 대상, [[TutorialPrefs]]와 같은 성격).
//
// 값은 StateFlow로 노출한다 — BgmPlayer가 이 값을 관찰해 즉시 재생/정지를 반영하고,
// 설정 다이얼로그의 스위치도 같은 소스를 구독한다.
@Singleton
class SettingsPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _bgmEnabled = MutableStateFlow(prefs.getBoolean(KEY_BGM_ENABLED, true))

    /** 배경 음악 재생 여부. 기본 켜짐. */
    val bgmEnabled: StateFlow<Boolean> = _bgmEnabled.asStateFlow()

    fun setBgmEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BGM_ENABLED, enabled) }
        _bgmEnabled.value = enabled
    }

    private companion object {
        const val KEY_BGM_ENABLED = "bgm_enabled"
    }
}
