package com.doge.simulator.data.local.snapshot

import kotlinx.serialization.json.Json

// 클라우드 세이브 payload 직렬화용 공용 Json.
// ignoreUnknownKeys: 신버전 앱이 추가한 필드를 구버전 앱이 무시하고 읽을 수 있게(전방 호환).
internal val snapshotJson: Json = Json {
    ignoreUnknownKeys = true
}
