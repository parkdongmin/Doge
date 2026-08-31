package com.doge.simulator.data.local.snapshot

import kotlinx.serialization.json.JsonObject

// 클라우드 스냅샷 JSON을 한 스키마 버전에서 다음 버전 모양으로 끌어올리는 순수 변환 모음.
//
// ── 스키마를 바꿀 때 규칙 ────────────────────────────────────────────
// 스냅샷에 들어가는 엔티티(GameSnapshot이 참조하는 *Entity)의 모양을 바꾸면
// GameSnapshot.SCHEMA_VERSION 을 +1 한다. 그리고:
//
//   - 필드 "추가"(기본값 있음) 또는 "삭제": step 등록 불필요.
//     역직렬화가 없는 필드는 기본값으로 채우고, ignoreUnknownKeys 가 사라진 필드를 무시한다.
//   - 필드 "이름 변경 / 타입 변경 / 테이블 분리 / 의미(단위) 변경": 옛 JSON을 손봐야 하므로
//     steps[이전버전] 에 (JsonObject) -> JsonObject 변환을 등록한다.
//
// 미래 버전(내가 아는 것보다 높은) 세이브는 다운그레이드하지 않고 복원을 건너뛴다.
internal object SnapshotMigrations {

    // key = 이 버전에서 (key+1)로 올리는 변환. 없는 버전은 "데이터 변형 불필요"로 간주하고 통과.
    private val steps: Map<Int, (JsonObject) -> JsonObject> = emptyMap()

    /**
     * [from] 버전 스냅샷 JSON을 [to] 버전 모양으로 변환한다.
     * - from == to  → 그대로
     * - from  > to  → null (미래 버전 세이브, 다운그레이드 불가)
     * - from  < to  → from..to-1 구간의 step 을 순서대로 적용(없으면 통과)
     */
    fun migrate(root: JsonObject, from: Int, to: Int): JsonObject? {
        if (from == to) return root
        if (from > to) return null
        var current = root
        for (version in from until to) {
            val step = steps[version]
            if (step != null) current = step(current)
        }
        return current
    }
}
