# Contract: ORBIT Domain UseCase API

이 앱은 외부에 노출하는 웹/네트워크 API가 없으므로, 여기서 "계약"은 **프레젠테이션 계층(ViewModel)이 의존하는 도메인 UseCase의 공개 함수 시그니처와 그 입출력 규약**을 말한다. 구현 시 이 계약을 어기지 않아야 `/speckit-tasks`에서 나눈 테스트가 유효하다.

## StartOrbitMatchUseCase

- **입력**: `bet: OrbitBet` (금액 + 위험도 티어)
- **사전조건**: 플레이어 재화 잔고 ≥ `bet.amount` (FR-013)
- **동작**: 재화에서 `bet.amount` 즉시 차감(FR-014) → 새 `OrbitMatchState` 생성(SIGNAL 0-0) → 17장 셔플, 각자 1장 배분, 첫 라운드 선공은 무작위(FR-001/FR-002)
- **출력**: `OrbitMatchState` (진행 중 상태)
- **실패 케이스**: 잔고 부족 시 상태 변경 없이 실패를 반환해야 한다(차감이 절대 먼저 일어나면 안 됨).

## PlayOrbitCardUseCase

- **입력**: 현재 `OrbitMatchState`, 사용할 `OrbitCard`(현재 턴 플레이어의 손패 중 하나), 카드 효과에 필요한 추가 선택(예: SCOUT_DRONE의 추측 Power, EMP/WARP_GATE의 대상)
- **사전조건**: 호출자는 `currentTurn`에 해당하는 플레이어와 일치해야 함. AI_CORE 강제 사용 규칙(FR-005) 위반 시(즉, AI_CORE를 낼 수 있는데 다른 카드를 내려 함) 실패.
- **동작**: 드로우(이미 턴 시작 시 수행됐다고 가정하거나, 이 유스케이스 내부에서 드로우까지 포함 — 구현 시 하나로 확정) → 카드 효과 적용(FR-005, SHIELD 무효화 규칙 FR-006 포함) → 사용된 카드를 `OrbitRoundState`(AI 판단용) 및 `cardsUsedThisRound`에 기록 → OUT 조건 충족 시 라운드 즉시 종료(FR-007) → 턴 교대
- **출력**: 갱신된 `OrbitMatchState` + "이번 턴에 공개할 카드/효과 결과" 를 나타내는 일회성 이벤트(프레젠테이션 계층에서 애니메이션 후 폐기, FR-004)
- CPU(B-01) 차례에는 내부적으로 `B01OrbitAi`가 이 유스케이스에 전달할 카드/선택을 결정한다(플레이어 손패 미참조, FR-010).

## ResolveOrbitRoundEndUseCase

- **입력**: 현재 `OrbitRoundState`
- **호출 시점**: 매 턴 시작 시 드로우 덱이 비어 있는지 확인하는 지점, 그리고 매 카드 효과 적용 직후 OUT 여부 확인 지점.
- **동작**:
  - 드로우 덱이 비어 있으면(FR-008): 마지막 손패 Power 비교 → 동점 시 `cardsUsedThisRound` Power 총합 비교 → 그것도 동점이면 DRAW.
  - OUT 발생 시: 즉시 해당 플레이어 패배, 상대 SIGNAL +1(FR-007).
- **출력**: `RoundEndReason`과 `winner`(DRAW면 null)가 채워진 `OrbitRoundState`. SIGNAL 3 도달 시 `OrbitMatchState.matchResult`도 함께 확정(FR-009).

## SettleOrbitBetUseCase

- **입력**: 종료된 `OrbitMatchState`(matchResult 확정 상태), 또는 "매치 도중 이탈" 신호
- **동작**: 승리 시 `bet.riskTier.winRewardMultiplier`에 따라 재화 지급(FR-015). 패배/이탈 시 추가 차감 없이 손실을 확정(FR-016, 이미 시작 시 차감됐으므로).
- **출력**: `BetSettlement { outcome, netChange }`

## ClaimOrbitDailyAdRewardUseCase

- **입력**: 없음(현재 시각/기기 로컬 날짜는 내부에서 조회)
- **사전조건**: `DailyRewardAdClaim.viewedCount < 5` (오늘 날짜 기준, FR-024)
- **동작**: 리워드 광고 시청 요청 → `RewardedAdResult`가 `Earned`일 때만 재화 500 지급 + `viewedCount += 1`(FR-023). `Dismissed`/`Failed`/`NotReady`는 아무 상태 변화 없음(FR-026).
- **출력**: 지급 성공 여부와 갱신된 `DailyRewardAdClaim`(또는 실패 사유).
