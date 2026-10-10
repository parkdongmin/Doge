package com.doge.simulator.domain.model.orbit

// 위험도(배율) 3단계. 베팅 금액과는 독립된 축으로, 티어가 높을수록 B-01의 실수 빈도
// (aiMistakeRate)가 낮아져 더 도전적인 상대가 되고, 승리 시 보상 배율(winRewardMultiplier)도
// 커진다. 가장 낮은 티어(SAFE)는 재화 규모와 무관하게 항상 선택 가능해야 한다(FR-012a).
//
// aiMaxMistakeGap: 실수할 때 최선보다 얼마나 나쁜 수까지 허용할지(B01OrbitAi 점수 단위, 100 = 라운드 승패).
// 안정은 눈에 보이는 실수도 하고, 고위험은 최선과 거의 차이 없는 수 사이에서만 흔들린다 — 처음 하는
// 사람이 룰을 몰라도 안정은 이길 만해야 하고, 고위험은 "이상한 판단"이 없어야 한다는 피드백.
// 배율은 OrbitDifficultySimTest로 맞춤(보통 실력 기준 승률 · 베팅당 기대 수익):
//   안정 75% · −7%   도전 59% · −2%   고위험 48% · 0%   (초보 −9% · −9% · −6%, 고수 −1% · +6% · +8%)
// 안정은 룰을 모르는 사람도 대부분 이기는 연습 모드라 보상을 작게, 고수는 어려울수록 이득이 나게
// 보통 실력이면 어느 난이도든 살짝 코인을 소모하고 안정이 가장 손해라 "안정만 최대 베팅 반복"이 정답이 아니다
enum class OrbitRiskTier(
    val displayName: String,
    val aiMistakeRate: Float,
    val aiMaxMistakeGap: Double,
    val winRewardMultiplier: Float
) {
    SAFE(displayName = "안정", aiMistakeRate = 1.0f, aiMaxMistakeGap = 100.0, winRewardMultiplier = 1.25f),
    CHALLENGE(displayName = "도전", aiMistakeRate = 0.7f, aiMaxMistakeGap = 60.0, winRewardMultiplier = 1.65f),
    HIGH_RISK(displayName = "고위험", aiMistakeRate = 0.3f, aiMaxMistakeGap = 20.0, winRewardMultiplier = 2.1f);

    companion object {
        // 항상 선택 가능해야 하는 최저 티어(FR-012a) — 베팅 없이 US1 단독 실행 시 기본값으로도 쓰인다.
        val LOWEST = SAFE
    }
}
