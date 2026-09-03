package com.doge.simulator.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// 클라우드 세이브 동기화 메타데이터(게임 진행상태 아님 → prefs로 충분, Room 마이그레이션 회피).
@Singleton
class CloudSyncPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("cloud_sync_prefs", Context.MODE_PRIVATE)

    // 이 기기 식별자 — 최초 1회 생성 후 고정. 클라우드 문서의 deviceId로 기록된다.
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit { putString(KEY_DEVICE_ID, it) }
        }

    // 이 기기가 마지막으로 적용(import 또는 push)한 클라우드 rev. 콜드 스타트 시
    // cloud.rev 가 이 값보다 크면 "다른 기기가 이후에 저장함" → import.
    var lastAppliedRev: Long
        get() = prefs.getLong(KEY_LAST_APPLIED_REV, -1L)
        set(value) = prefs.edit { putLong(KEY_LAST_APPLIED_REV, value) }

    var lastPushAttemptAt: Long
        get() = prefs.getLong(KEY_LAST_PUSH_ATTEMPT_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_PUSH_ATTEMPT_AT, value) }

    // 로그아웃 시 호출 — deviceId는 기기 고유값이라 유지하고, rev 추적만 지운다.
    // 안 지우면 로컬(방금 비운 DB)이 다음 로그인 계정의 클라우드 rev보다 높게 남아
    // restore()가 "로컬이 최신"으로 오판, 빈 데이터를 그 계정 세이브에 덮어쓸 위험이 있다.
    fun resetSyncState() {
        prefs.edit {
            remove(KEY_LAST_APPLIED_REV)
            remove(KEY_LAST_PUSH_ATTEMPT_AT)
        }
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_LAST_APPLIED_REV = "last_applied_rev"
        const val KEY_LAST_PUSH_ATTEMPT_AT = "last_push_attempt_at"
    }
}
