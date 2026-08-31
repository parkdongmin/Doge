package com.doge.simulator.data.local.snapshot

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

// 클라우드 payload(JSON 문자열)를 현재 앱이 쓸 수 있는 GameSnapshot으로 읽어들인다.
// 파싱 → (필요 시) 스키마 마이그레이션 → 역직렬화 순.
internal sealed interface SnapshotLoad {
    data class Loaded(val snapshot: GameSnapshot) : SnapshotLoad

    /** payload 자체가 깨졌거나 JSON이 아님 — 부분 다운로드 등 일시적 문제일 수 있어 재시도 대상. */
    data object Corrupt : SnapshotLoad

    /** 미래 버전 세이브거나, 마이그레이션 후에도 이 앱 스키마로 못 맞춤 — 재시도해도 안 됨(로컬 유지). */
    data object Incompatible : SnapshotLoad
}

internal fun loadSnapshot(payloadJson: String): SnapshotLoad {
    val root = runCatching { snapshotJson.parseToJsonElement(payloadJson) as? JsonObject }
        .getOrNull() ?: return SnapshotLoad.Corrupt

    val savedVersion = (root["snapshotSchemaVersion"] as? JsonPrimitive)?.intOrNull ?: 1
    val migrated = SnapshotMigrations.migrate(root, savedVersion, GameSnapshot.SCHEMA_VERSION)
        ?: return SnapshotLoad.Incompatible

    val snapshot = runCatching { snapshotJson.decodeFromJsonElement<GameSnapshot>(migrated) }
        .getOrNull() ?: return SnapshotLoad.Incompatible

    return SnapshotLoad.Loaded(snapshot)
}
