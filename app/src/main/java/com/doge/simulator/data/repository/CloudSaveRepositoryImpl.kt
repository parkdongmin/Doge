package com.doge.simulator.data.repository

import com.doge.simulator.domain.model.CloudSave
import com.doge.simulator.domain.repository.CloudSaveRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// saves/{uid} 문서 1개에 게임 스냅샷 전체를 JSON 문자열로 저장.
// 필드가 몇 개 바뀌든 문서 쓰기 1회 → 비용은 세션당 push 횟수에만 비례.
class CloudSaveRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CloudSaveRepository {

    private val saves get() = firestore.collection("saves")

    override suspend fun pull(uid: String): Result<CloudSave?> = runCatching {
        val doc = saves.document(uid).get().await()
        if (!doc.exists()) return@runCatching null
        val payload = doc.getString(FIELD_PAYLOAD) ?: return@runCatching null
        CloudSave(
            rev = doc.getLong(FIELD_REV) ?: 0L,
            updatedAt = doc.getLong(FIELD_UPDATED_AT) ?: 0L,
            roomDbVersion = (doc.getLong(FIELD_ROOM_DB_VERSION) ?: 0L).toInt(),
            snapshotSchemaVersion = (doc.getLong(FIELD_SCHEMA_VERSION) ?: 0L).toInt(),
            deviceId = doc.getString(FIELD_DEVICE_ID) ?: "",
            payloadJson = payload
        )
    }

    override suspend fun push(uid: String, save: CloudSave): Result<Unit> = runCatching {
        val data = mapOf(
            FIELD_REV to save.rev,
            FIELD_UPDATED_AT to save.updatedAt,
            FIELD_ROOM_DB_VERSION to save.roomDbVersion,
            FIELD_SCHEMA_VERSION to save.snapshotSchemaVersion,
            FIELD_DEVICE_ID to save.deviceId,
            FIELD_PAYLOAD to save.payloadJson
        )
        saves.document(uid).set(data).await()
        Unit
    }

    private companion object {
        const val FIELD_REV = "rev"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_ROOM_DB_VERSION = "roomDbVersion"
        const val FIELD_SCHEMA_VERSION = "snapshotSchemaVersion"
        const val FIELD_DEVICE_ID = "deviceId"
        const val FIELD_PAYLOAD = "payload"
    }
}
