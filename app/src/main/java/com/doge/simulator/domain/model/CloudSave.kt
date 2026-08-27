package com.doge.simulator.domain.model

// Firestore saves/{uid} 문서 1개에 담기는 클라우드 세이브.
// payloadJson = GameSnapshot을 kotlinx.serialization JSON으로 직렬화한 문자열(불투명).
data class CloudSave(
    val rev: Long,                  // 단조 증가. push할 때마다 +1
    val updatedAt: Long,            // client millis (기존 리더보드 컨벤션과 동일)
    val roomDbVersion: Int,
    val snapshotSchemaVersion: Int,
    val deviceId: String,           // 마지막으로 push한 기기 (충돌 UI/디버깅용)
    val payloadJson: String
)
