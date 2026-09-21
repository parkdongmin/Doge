package com.doge.simulator.domain.model.orbit

// 위험도(배율) 3단계. 베팅 금액과는 독립된 축으로, 티어가 높을수록 B-01의 실수 빈도
// (aiMistakeRate)가 낮아져 더 도전적인 상대가 되고, 승리 시 보상 배율(winRewardMultiplier)도
// 커진다. 가장 낮은 티어(SAFE)는 재화 규모와 무관하게 항상 선택 가능해야 한다(FR-012a).
// 정확한 수치는 플레이테스트를 통해 추후 조정한다(spec.md Assumptions 참고).
enum class OrbitRiskTier(
    val displayName: String,
    val aiMistakeRate: Float,
    val winRewardMultiplier: Float
) {
    SAFE(displayName = "안정", aiMistakeRate = 0.45f, winRewardMultiplier = 1.2f),
    CHALLENGE(displayName = "도전", aiMistakeRate = 0.25f, winRewardMultiplier = 1.6f),
    HIGH_RISK(displayName = "고위험", aiMistakeRate = 0.10f, winRewardMultiplier = 2.2f);

    companion object {
        // 항상 선택 가능해야 하는 최저 티어(FR-012a) — 베팅 없이 US1 단독 실행 시 기본값으로도 쓰인다.
        val LOWEST = SAFE
    }
}
