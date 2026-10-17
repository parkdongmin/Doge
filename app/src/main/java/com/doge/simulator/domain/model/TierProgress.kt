package com.doge.simulator.domain.model

// 탐사 티어 해금 조건 — GameConstants.TIER_UNLOCK_CONDITIONS
sealed interface TierUnlockCondition {
    val label: String
    val required: Int
    fun current(progress: TierProgress): Int

    // T3~T10: 도감에 그 등급 이상 행성이 N개
    data class RarityDiscovered(val rarity: RarityTier, override val required: Int, override val label: String) : TierUnlockCondition {
        override fun current(progress: TierProgress) = progress.discoveredVariantIds.count { variantId ->
            val found = PlanetMetaDataTable.variantRarity[variantId]
            found != null && found.ordinal >= rarity.ordinal
        }
    }

    // T11: 1부 완주
    data object Part1Completed : TierUnlockCondition {
        override val label = "1부 완주"
        override val required = 1
        override fun current(progress: TierProgress) = if (progress.part1Completed) 1 else 0
    }

    // T12·T14: 특성 행성 N개 획득
    data class TraitPlanetsAcquired(override val required: Int) : TierUnlockCondition {
        override val label = "특성 행성 ${required}개 획득"
        override fun current(progress: TierProgress) = progress.traitPlanetsAcquired
    }

    // T13·T15: 적합도 높음 + 등급 N 이상 좋은 특성이 붙은 행성 M개 획득
    data class HighFitTraitAcquired(val minGrade: Int, override val required: Int) : TierUnlockCondition {
        override val label = if (minGrade >= 3) "적합도 높음 · Ⅲ등급 특성 행성 ${required}개"
                              else "적합도 높음 · Ⅱ등급 이상 특성 행성 ${required}개"
        override fun current(progress: TierProgress) =
            if (minGrade >= 3) progress.highFitTier3Acquired else progress.highFitTier2Acquired
    }
}

// 티어 해금 판정에 필요한 영구 기록 묶음. 탐사 화면 배너·티어 칩·파견 검증이 모두 이 한 객체로 판정한다.
//
// 기준은 "현재 보유 중인 행성"이 아니라 "한 번이라도 얻은 기록"(도감, 특성 행성 획득 카운터)이다 —
// 매도는 이 게임의 정상적인 플레이(손절/차익실현)인데, 보유 개수 기준이면 행성 하나 파는 순간 사다리 위
// 티어가 전부 같이 잠겨버리는 문제가 있었음(2026-09-04). 도감·획득 기록은 매도로 흔들리지 않는다.
data class TierProgress(
    val discoveredVariantIds: Set<String> = emptySet(),
    val part1Completed: Boolean = false,
    val traitPlanetsAcquired: Int = 0,
    val highFitTier2Acquired: Int = 0,
    val highFitTier3Acquired: Int = 0
) {
    // 티어는 사다리처럼 순서대로만 열린다 — 각 티어 조건은 서로 독립적이라, 운 좋게 뒤 티어 조건을 먼저
    // 채우고 앞 티어 조건은 못 채운 "구멍"이 생길 수 있다(예: 언커먼 1개만 있으면 T5 자체 조건은 만족하지만
    // T4의 "커먼 이상 3개+"는 아직 부족). 그래서 1..maxTier를 순서대로 훑어 "가장 먼저 막힌 티어"를 찾고,
    // 그 이전 티어까지만 실제로 열린 것으로 친다. null = maxTier까지 전부 열림.
    fun firstLockedTier(maxTier: Int = GameConstants.MAX_TIER): Int? {
        for (t in 1..maxTier) {
            val condition = GameConstants.TIER_UNLOCK_CONDITIONS[t] ?: continue
            if (condition.current(this) < condition.required) return t
        }
        return null
    }

    fun isUnlocked(tier: Int): Boolean = firstLockedTier(maxTier = tier) == null

    // (현재, 필요) — 배너 진행도 표시용. 조건 없는 티어는 null
    fun progressFor(tier: Int): Pair<Int, Int>? {
        val condition = GameConstants.TIER_UNLOCK_CONDITIONS[tier] ?: return null
        return condition.current(this).coerceAtMost(condition.required) to condition.required
    }
}

// 유저 정보에 저장된 특성 행성 획득 기록(매도해도 줄지 않음)
data class TraitAcquisitionCounts(
    val traitPlanets: Int = 0,
    val highFitTier2: Int = 0,
    val highFitTier3: Int = 0
)
