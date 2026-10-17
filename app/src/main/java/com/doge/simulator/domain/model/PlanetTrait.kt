package com.doge.simulator.domain.model

// ── 2부 행성 특성 (2026-10-14) ───────────────────────────────────────
// T11 이상 탐사에서 발견한 행성에만 붙는 성질. 새 자원·새 행성 종류 없이 기존 시스템(생산, 이벤트,
// 고장/정비, 자원 드롭, 강화)의 수치 하나씩을 바꾼다. 수치 원본은 specs/004-part2-planet-traits/balance.md

// 특성이 바꾸는 대상. 반대·중복 금지 규칙 때문에 한 행성의 한 축에는 특성이 최대 1개만 붙는다
enum class TraitAxis {
    PRODUCTION,
    BAD_CHANCE,
    BREAKDOWN_THRESHOLD,
    BOOM,
    RESOURCE_DROP,
    UPGRADE_SUCCESS
}

enum class TraitType(val displayName: String, val isGood: Boolean, val axis: TraitAxis) {
    HIGH_PRODUCTION("높은 생산력", true, TraitAxis.PRODUCTION),
    STABLE_ENVIRONMENT("안정된 환경", true, TraitAxis.BAD_CHANCE),
    STURDY_STRUCTURE("튼튼한 구조", true, TraitAxis.BREAKDOWN_THRESHOLD),
    BOOM_PRONE("호황 체질", true, TraitAxis.BOOM),
    RICH_RESOURCES("풍부한 자원", true, TraitAxis.RESOURCE_DROP),
    UPGRADE_APTITUDE("강화 적성", true, TraitAxis.UPGRADE_SUCCESS),

    LOW_PRODUCTION("낮은 생산력", false, TraitAxis.PRODUCTION),
    UNSTABLE_ENVIRONMENT("불안정한 환경", false, TraitAxis.BAD_CHANCE),
    WEAK_STRUCTURE("약한 구조", false, TraitAxis.BREAKDOWN_THRESHOLD),
    BARREN_RESOURCES("메마른 자원", false, TraitAxis.RESOURCE_DROP),
    UPGRADE_INAPTITUDE("강화 부적성", false, TraitAxis.UPGRADE_SUCCESS);

    // 같은 축의 반대 특성(호황 체질은 반대 없음) — 한 행성에 함께 붙지 않는다
    val opposite: TraitType?
        get() = entries.firstOrNull { it.axis == axis && it.isGood != isGood }

    companion object {
        val GOOD: List<TraitType> = entries.filter { it.isGood }
        val BAD: List<TraitType> = entries.filter { !it.isGood }
    }
}

// 행성 종류 × 좋은 특성의 궁합. 플레이어에게는 별이 아니라 글자로 보인다
enum class TraitFit(val label: String) {
    HIGH("높음"),
    MEDIUM("보통"),
    LOW("낮음")
}

// intensity: 좋은 특성의 등급 1~3(Ⅰ/Ⅱ/Ⅲ). 나쁜 특성은 등급이 없어 항상 1로 두고 쓰지 않는다
data class PlanetTrait(val type: TraitType, val intensity: Int = 1) {
    val intensityLabel: String
        get() = if (type.isGood) ROMAN.getOrElse(intensity - 1) { "" } else ""

    fun conflictsWith(other: TraitType): Boolean = type == other || type.opposite == other

    companion object {
        private val ROMAN = listOf("Ⅰ", "Ⅱ", "Ⅲ")

        // DB·스냅샷 저장 형식: "HIGH_PRODUCTION:3,UNSTABLE_ENVIRONMENT:1" (resourcesResult와 같은 콤마 문자열).
        // 모르는 이름·깨진 조각은 건너뛴다 — 나중에 특성을 추가해도 옛 앱이 죽지 않게
        fun encode(traits: List<PlanetTrait>): String =
            traits.joinToString(",") { "${it.type.name}:${it.intensity}" }

        fun decode(raw: String): List<PlanetTrait> {
            if (raw.isBlank()) return emptyList()
            return raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                val type = runCatching { TraitType.valueOf(parts[0].trim()) }.getOrNull() ?: return@mapNotNull null
                val intensity = parts.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(1, 3) ?: 1
                PlanetTrait(type, if (type.isGood) intensity else 1)
            }
        }
    }
}
