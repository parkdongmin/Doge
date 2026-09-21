# Phase 1 Data Model: ORBIT — Crew Lounge Card Mini-Game

스펙(`spec.md`)의 Key Entities를 기반으로 도메인 모델을 구체화한다. 아래는 필드/관계/상태 전이를 설명하는 논리적 모델이며, 구체적인 Kotlin 타입 선언은 구현 단계(`/speckit-tasks` 이후)에서 결정한다.

## OrbitCardType (열거)

8종 카드 종류. 각 종류는 고정된 Power와 매수를 가진다.

| 종류 | Power | 매수 | 비고 |
|---|---|---|---|
| SCOUT_DRONE | 1 | 4 | Power 1은 스스로 지목 불가 |
| SENSOR | 2 | 3 | |
| PROBE | 3 | 2 | |
| SHIELD | 4 | 2 | |
| EMP | 5 | 2 | 덱에 최소 1장 있어야 사용 가능 |
| WARP_GATE | 6 | 2 | |
| AI_CORE | 7 | 1 | EMP/WARP_GATE와 동시 보유 시 강제 사용 |
| CAPTAIN | 8 | 1 | 직접 사용/버림 시 즉시 OUT (교환 제외) |

- 총 17장. GLITCH(Power 0)는 이번 스코프 제외(스펙 Assumptions).

## OrbitCard

- `type: OrbitCardType`
- `power: Int` (OrbitCardType에서 파생, 중복 저장하지 않아도 됨)

## OrbitDeck

- `drawPile: List<OrbitCard>` — 셔플된 상태에서 순서대로 소진되는 드로우 가능 카드.
- `usedCards: List<OrbitCard>` (내부 전용) — 지금까지 사용된 카드. **AI 판단용으로만 사용되며 플레이어 UI에 지속 노출되지 않음**(data-model.md 참고 사항: 프레젠테이션 계층에서 이 필드를 구독하지 않도록 문서화 필요).
- 불변식: `drawPile.size + usedCards.size + (양쪽 플레이어 손패 수) == 17` (라운드 시작 시점 기준).
- 상태 전이: `shuffle()` (라운드 시작) → `draw()` (drawPile에서 1장 제거) → `markUsed(card)` (usedCards에 추가).

## OrbitPlayerState

- `hand: List<OrbitCard>` (0~2장, 정상 진행 중에는 1장 또는 2장)
- `isOut: Boolean`
- `shieldActive: Boolean`, `shieldExpiresAtOwnNextTurnStart: Boolean` — SHIELD 효과는 "자신의 다음 턴 시작"까지 유지되므로 만료 조건을 턴 이벤트로 표현.
- `cardsUsedThisRound: List<OrbitCard>` — 덱 소진 시 Power 총합 비교(FR-008)에 사용.

## OrbitRoundState

- `roundNumber: Int`
- `deck: OrbitDeck`
- `players: Map<PlayerSide, OrbitPlayerState>` (PlayerSide = PLAYER | B01)
- `currentTurn: PlayerSide`
- `firstPlayerOfRound: PlayerSide` (직전 라운드 패자 규칙 적용 결과, FR-002)
- `endReason: RoundEndReason?` (OUT | DECK_EXHAUSTED | DRAW), null이면 진행 중
- `winner: PlayerSide?` (DRAW인 경우 null)

상태 전이: `NotStarted → InProgress(턴 반복) → Ended(endReason, winner)`. `Ended(DRAW)`가 되면 새 `OrbitRoundState`를 즉시 재생성(SIGNAL 변동 없음).

## OrbitMatchState

- `signals: Map<PlayerSide, Int>` (0~3)
- `currentRound: OrbitRoundState`
- `bet: OrbitBet`
- `matchResult: MatchResult?` (WON | LOST), null이면 진행 중. 3 SIGNAL 선취 시 확정(FR-009).

## OrbitRiskTier (열거, 3단계)

- `tierLevel: Int` (1~3)
- `aiMistakeRate: Float` — B-01이 최적이 아닌 수를 선택할 확률. 값 자체는 스펙 Assumptions에 따라 추후 플레이테스트로 확정(플레이스홀더 허용).
- `winRewardMultiplier: Float` — 승리 시 베팅액 대비 보상 배율. 정확한 값은 추후 경제 밸런싱에서 확정.
- 불변식: `tierLevel = 1`(최저 티어)은 항상 선택 가능해야 함(FR-012a, 재화 규모와 무관).

## OrbitBet

- `amount: Long` (고정 4단계 금액 중 하나: 500 / 1,000 / 2,500 / 5,000)
- `riskTier: OrbitRiskTier`
- `settledResult: BetSettlement?` — 매치 종료 후 확정되는 `{ outcome: WIN|LOSS, netChange: Long }`.

## Wallet(재화) — 기존 엔티티 재사용

- ORBIT은 기존 재화 저장소에 대해 "차감"(매치 시작 시, FR-014)과 "지급"(승리 시 FR-015, 리워드 광고 시 FR-023)만 수행하는 소비자다. 신규 필드/테이블을 추가하지 않는다.

## DailyRewardAdClaim

- `viewedCount: Int` (0~5)
- `lastResetLocalDate: String` (예: `yyyy-MM-dd`, 기기 로컬 타임존 기준)
- 조회 시 오늘 날짜와 `lastResetLocalDate`가 다르면 `viewedCount = 0`으로 리셋 후 갱신(FR-025).
- 저장 위치: 기존 `AdFrequencyGate`와 동일하게 로컬 `SharedPreferences`(클라우드 세이브 미동기화, 스펙에서 확정).

## B01OrbitAi (판단 로직, 데이터라기보다 서비스이지만 상태 의존성이 있어 함께 기술)

- 입력: 공개된 정보만 — `usedCards`(카드 목록), Sensor로 확인한 상대 카드(알고 있다면), Warp Gate로 교환된 카드 이력, 상대 카드 변경 여부(변경 시 내부적으로 UNKNOWN 처리), 남은 덱 추정, 상대 SHIELD 상태, 자신의 현재 손패.
- 출력: 이번 턴에 사용할 카드 선택 + (필요 시) 대상/추측값 선택.
- 내부적으로는 항상 "이론적 최적 수"를 계산하되, `aiMistakeRate` 확률로 차선책 중 하나를 대신 선택(FR-011).
- 플레이어의 실제 손패는 절대 참조하지 않는다(FR-010) — 이는 구현 시 테스트로 강제 검증되어야 하는 불변식.

## 엔티티 관계 요약

```
OrbitMatchState 1 ── 1 OrbitBet ── 1 OrbitRiskTier
       │
       └── 1 OrbitRoundState (현재 라운드, 종료 시 교체)
                 │
                 ├── 1 OrbitDeck
                 └── 2 OrbitPlayerState (PLAYER, B01)
```
