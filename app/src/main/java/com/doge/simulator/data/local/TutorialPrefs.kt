package com.doge.simulator.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// 첫 진입 튜토리얼 진행 플래그. 게임 진행상태가 아니라 "이 기기에서 안내를 봤는지"라
// prefs로 충분하고 클라우드 세이브에도 안 실린다([[project_cloud_save]] 스냅샷 제외 대상).
//
// 각 플래그는 StateFlow로 노출한다 — TutorialViewModel이 이 값들을 combine으로 관찰하므로,
// ExploreViewModel이 신규 게임에서 startedFresh를 켜는 순간 튜토리얼 판정이 즉시 다시 돈다.
// (예전엔 non-reactive 읽기라, 로그인 직후엔 안 뜨고 탭을 바꿨다 와야 떴음)
@Singleton
class TutorialPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)

    private val _startedFresh = MutableStateFlow(prefs.getBoolean(KEY_STARTED_FRESH, false))
    private val _part1Done = MutableStateFlow(prefs.getBoolean(KEY_PART1, false))
    private val _part2Done = MutableStateFlow(prefs.getBoolean(KEY_PART2, false))
    private val _hqIntroDone = MutableStateFlow(prefs.getBoolean(KEY_HQ_INTRO, false))
    private val _upgradeIntroDone = MutableStateFlow(prefs.getBoolean(KEY_UPGRADE_INTRO, false))

    /** 이 기기에서 완전 신규 게임으로 시작했는지(= 스타터 자산을 지급받았는지).
     *  클라우드 세이브로 복원된 기존 유저는 false로 남아 튜토리얼이 아예 안 뜬다. */
    val startedFresh: StateFlow<Boolean> = _startedFresh.asStateFlow()
    val part1Done: StateFlow<Boolean> = _part1Done.asStateFlow()
    val part2Done: StateFlow<Boolean> = _part2Done.asStateFlow()
    val hqIntroDone: StateFlow<Boolean> = _hqIntroDone.asStateFlow()
    val upgradeIntroDone: StateFlow<Boolean> = _upgradeIntroDone.asStateFlow()

    fun markStartedFresh() = set(KEY_STARTED_FRESH, _startedFresh)
    fun markPart1Done() = set(KEY_PART1, _part1Done)
    fun markPart2Done() = set(KEY_PART2, _part2Done)
    fun markHqIntroDone() = set(KEY_HQ_INTRO, _hqIntroDone)
    fun markUpgradeIntroDone() = set(KEY_UPGRADE_INTRO, _upgradeIntroDone)

    private fun set(key: String, flow: MutableStateFlow<Boolean>) {
        prefs.edit { putBoolean(key, true) }
        flow.value = true
    }

    private companion object {
        const val KEY_STARTED_FRESH = "started_fresh"
        const val KEY_PART1 = "part1_done"
        const val KEY_PART2 = "part2_done"
        const val KEY_HQ_INTRO = "hq_intro_done"
        const val KEY_UPGRADE_INTRO = "upgrade_intro_done"
    }
}
