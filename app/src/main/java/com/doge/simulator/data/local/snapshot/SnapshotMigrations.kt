package com.doge.simulator.data.local.snapshot

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.roundToLong

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
    private val steps: Map<Int, (JsonObject) -> JsonObject> = mapOf(
        // 2 → 3: 행성 고장/정비 시스템. Room MIGRATION_20_21과 같은 변환 — 음수 생산 배율은 고장
        // (손해 배율 1.0)으로, 배율은 0.5~2.0으로, 시세 보정은 바뀐 배율로 다시 계산.
        // (v1 세이브도 v2 단계는 변형이 없어 그대로 통과한 뒤 여기서 변환된다)
        2 to { root -> root.mapPlanets(::migratePlanetToBrokenState) }
    )

    private fun JsonObject.mapPlanets(transform: (JsonObject) -> JsonObject): JsonObject {
        val planets = this["planets"] as? JsonArray ?: return this
        return JsonObject(this + ("planets" to JsonArray(planets.map { (it as? JsonObject)?.let(transform) ?: it })))
    }

    private fun migratePlanetToBrokenState(planet: JsonObject): JsonObject {
        val oldMultiplier = planet["productionMultiplier"]?.jsonPrimitive?.doubleOrNull ?: 1.0
        val multiplier = oldMultiplier.coerceIn(0.5, 2.0)
        val buyPrice = planet["buyPrice"]?.jsonPrimitive?.longOrNull ?: 0L
        val upgradeInvestment = planet["upgradeInvestment"]?.jsonPrimitive?.longOrNull ?: 0L
        val deviation = multiplier - 1.0
        val marketAdjustment = (deviation * buyPrice + deviation * upgradeInvestment * 0.25).roundToLong()
        return JsonObject(
            planet + mapOf(
                "productionMultiplier" to JsonPrimitive(multiplier),
                "marketAdjustment" to JsonPrimitive(marketAdjustment),
                "lossMultiplier" to JsonPrimitive(if (oldMultiplier < 0) 1.0 else 0.0)
            )
        )
    }

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
