package com.doge.simulator.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 첫 진입 튜토리얼 진행 플래그. 게임 진행상태가 아니라 "이 기기에서 안내를 봤는지"라
// prefs로 충분하고 클라우드 세이브에도 안 실린다([[project_cloud_save]] 스냅샷 제외 대상).
// 새 기기에선 다시 보일 수 있지만, 표시 조건을 게임 상태로도 게이팅해서(진행 중 탐사/보유
// 행성 유무) 클라우드 복원 유저에겐 사실상 안 뜬다.
@Singleton
class TutorialPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)

    // 이 기기에서 완전 신규 게임으로 시작했는지(= 스타터 자산을 지급받았는지).
    // 클라우드 세이브로 복원된 기존 유저는 false로 남아 튜토리얼이 아예 안 뜬다.
    var startedFresh: Boolean
        get() = prefs.getBoolean(KEY_STARTED_FRESH, false)
        set(value) = prefs.edit { putBoolean(KEY_STARTED_FRESH, value) }

    // Part 1: 탐사 파견 안내 완료
    var part1Done: Boolean
        get() = prefs.getBoolean(KEY_PART1, false)
        set(value) = prefs.edit { putBoolean(KEY_PART1, value) }

    // Part 2: 방치수익 안내 완료
    var part2Done: Boolean
        get() = prefs.getBoolean(KEY_PART2, false)
        set(value) = prefs.edit { putBoolean(KEY_PART2, value) }

    // 맥락형 원샷 버블 — 해당 화면 첫 방문 시 1회.
    var hqIntroDone: Boolean
        get() = prefs.getBoolean(KEY_HQ_INTRO, false)
        set(value) = prefs.edit { putBoolean(KEY_HQ_INTRO, value) }

    var upgradeIntroDone: Boolean
        get() = prefs.getBoolean(KEY_UPGRADE_INTRO, false)
        set(value) = prefs.edit { putBoolean(KEY_UPGRADE_INTRO, value) }

    private companion object {
        const val KEY_STARTED_FRESH = "started_fresh"
        const val KEY_PART1 = "part1_done"
        const val KEY_PART2 = "part2_done"
        const val KEY_HQ_INTRO = "hq_intro_done"
        const val KEY_UPGRADE_INTRO = "upgrade_intro_done"
    }
}
