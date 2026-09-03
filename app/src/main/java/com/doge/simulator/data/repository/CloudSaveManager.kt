package com.doge.simulator.data.repository

import com.doge.simulator.data.local.CloudSyncPrefs
import com.doge.simulator.data.local.snapshot.LocalSnapshotDataSource
import com.doge.simulator.data.local.snapshot.SnapshotLoad
import com.doge.simulator.data.local.snapshot.loadSnapshot
import com.doge.simulator.data.local.snapshot.snapshotJson
import com.doge.simulator.domain.model.CloudSave
import com.doge.simulator.domain.repository.AuthRepository
import com.doge.simulator.domain.repository.CloudSaveRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

enum class CloudRestoreResult {
    IMPORTED,          // 클라우드가 더 최신 → 로컬에 덮어씀
    LOCAL_KEPT,        // 로컬이 최신 이상 → 클라우드로 밀어올림
    NO_CLOUD,          // 클라우드에 세이브 없음 → 로컬을 첫 세이브로 push
    SKIPPED_VERSION,   // 스키마가 이 앱보다 미래거나 마이그레이션 불가 → 아무것도 안 함(로컬 유지)
    FAILED             // 네트워크/파싱 실패 → 로컬 유지, 다음 기회에 재시도
}

// 스냅샷 export/import + Firestore push/pull을 하나의 락으로 직렬화하는 조정자.
// (60초 방치수익 루프, ON_STOP push, 스플래시 복원이 동시에 export/import를 건드릴 수 있음)
@Singleton
class CloudSaveManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val snapshotSource: LocalSnapshotDataSource,
    private val cloudSaveRepository: CloudSaveRepository,
    private val syncPrefs: CloudSyncPrefs
) {
    private val mutex = Mutex()

    /** 로컬 상태를 클라우드로 밀어올린다. 백그라운드 트리거용 — 실패는 조용히 무시. */
    suspend fun push(): Boolean = mutex.withLock { pushLocked() }

    /** 로그아웃: 마지막 push 시도 → 로컬 게임 데이터·동기화 메타데이터 정리 → Firebase 로그아웃.
     * push가 실패해도(네트워크 등) 로그아웃 자체는 막지 않는다 — 마지막 저장 시점 데이터로 남는다. */
    suspend fun signOutAndClearLocal() = mutex.withLock {
        runCatching { pushLocked() }
        snapshotSource.clearLocal()
        syncPrefs.resetSyncState()
        authRepository.signOut()
    }

    /** 스플래시 진입 시 1회. LWW: 클라우드 rev가 이 기기가 마지막으로 적용한 rev보다 크면 import. */
    suspend fun restore(): CloudRestoreResult = mutex.withLock {
        val uid = authRepository.getCurrentUser()?.uid ?: return@withLock CloudRestoreResult.FAILED
        val cloud = cloudSaveRepository.pull(uid).getOrElse { return@withLock CloudRestoreResult.FAILED }
            ?: run {
                pushLocked()
                return@withLock CloudRestoreResult.NO_CLOUD
            }
        if (cloud.rev > syncPrefs.lastAppliedRev) {
            // 스키마 버전이 달라도 마이그레이션으로 맞출 수 있으면 맞춘다.
            // (roomDbVersion은 더 이상 게이트가 아님 — 참고용으로만 저장됨)
            val snapshot = when (val load = loadSnapshot(cloud.payloadJson)) {
                SnapshotLoad.Corrupt -> return@withLock CloudRestoreResult.FAILED
                SnapshotLoad.Incompatible -> return@withLock CloudRestoreResult.SKIPPED_VERSION
                is SnapshotLoad.Loaded -> load.snapshot
            }
            snapshotSource.import(snapshot)
            syncPrefs.lastAppliedRev = cloud.rev
            return@withLock CloudRestoreResult.IMPORTED
        }
        pushLocked()
        CloudRestoreResult.LOCAL_KEPT
    }

    private suspend fun pushLocked(): Boolean {
        val uid = authRepository.getCurrentUser()?.uid ?: return false
        syncPrefs.lastPushAttemptAt = System.currentTimeMillis()
        val snapshot = snapshotSource.export()
        val nextRev = maxOf(syncPrefs.lastAppliedRev, 0L) + 1
        val save = CloudSave(
            rev = nextRev,
            updatedAt = System.currentTimeMillis(),
            roomDbVersion = snapshot.roomDbVersion,
            snapshotSchemaVersion = snapshot.snapshotSchemaVersion,
            deviceId = syncPrefs.deviceId,
            payloadJson = snapshotJson.encodeToString(snapshot)
        )
        return cloudSaveRepository.push(uid, save)
            .onSuccess { syncPrefs.lastAppliedRev = nextRev }
            .isSuccess
    }
}
