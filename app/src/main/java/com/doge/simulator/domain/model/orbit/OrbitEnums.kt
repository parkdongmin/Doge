package com.doge.simulator.domain.model.orbit

// 라운드 종료 사유. OUT은 카드 효과로 즉시 결정, DECK_EXHAUSTED는 손패/사용카드 Power
// 비교로 승자가 갈린 경우, DRAW는 그 비교까지 전부 동점인 경우(FR-008).
enum class RoundEndReason { OUT, DECK_EXHAUSTED, DRAW }

// 매치(여러 라운드) 최종 결과. SIGNAL 3개를 먼저 모은 쪽 기준으로 PLAYER 시점에서 판정한다.
enum class MatchOutcome { WON, LOST }
