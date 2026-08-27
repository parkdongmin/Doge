package com.doge.simulator.data.repository

import com.doge.simulator.data.local.CloudSyncPrefs
import com.doge.simulator.data.local.snapshot.GameSnapshot
import com.doge.simulator.data.local.snapshot.LocalSnapshotDataSource
import com.doge.simulator.data.local.snapshot.snapshotJson
import com.doge.simulator.domain.model.CloudSave
import com.doge.simulator.domain.repository.AuthRepository
import com.doge.simulator.domain.repository.CloudSaveRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

enum class CloudRestoreResult {
    IMPORTED,          // 클라우드가 더 최신 → 로컬에 덮어씀
    LOCAL_KEPT,        // 로컬이 최신 이상 → 클라우드로 밀어올림
    NO_CLOUD,          // 클라우드에 세이브 없음 → 로컬을 첫 세이브로 push
    SKIPPED_VERSION,   // roomDbVersion 불일치 → 아무것도 안 함(로컬 유지)
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

    /** 스플래시 진입 시 1회. LWW: 클라우드 rev가 이 기기가 마지막으로 적용한 rev보다 크면 import. */
    suspend fun restore(): CloudRestoreResult = mutex.withLock {
        val uid = authRepository.getCurrentUser()?.uid ?: return@withLock CloudRestoreResult.FAILED
        val cloud = cloudSaveRepository.pull(uid).getOrElse { return@withLock CloudRestoreResult.FAILED }
            ?: run {
                pushLocked()
                return@withLock CloudRestoreResult.NO_CLOUD
            }
        if (cloud.roomDbVersion != snapshotSource.roomDbVersion) {
            return@withLock CloudRestoreResult.SKIPPED_VERSION
        }
        if (cloud.rev > syncPrefs.lastAppliedRev) {
            val snapshot = runCatching { snapshotJson.decodeFromString<GameSnapshot>(cloud.payloadJson) }
                .getOrElse { return@withLock CloudRestoreResult.FAILED }
            if (snapshot.roomDbVersion != snapshotSource.roomDbVersion) {
                return@withLock CloudRestoreResult.SKIPPED_VERSION
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
