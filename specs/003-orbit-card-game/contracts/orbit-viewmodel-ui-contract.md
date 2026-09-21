# Contract: ORBIT ViewModel ↔ UI Contract

Compose 화면(`presentation/screen/orbit/*`)이 `OrbitViewModel`로부터 구독하는 상태와, 반대로 UI가 ViewModel에 보내는 사용자 의도(Intent)를 정의한다. 이 문서는 화면 구현 시 어떤 상태/이벤트가 필요한지에 대한 계약이며, 정확한 Kotlin 타입은 구현 단계에서 확정한다.

## 화면별 필요 상태 (State)

### LoungeScreen
- B-01 대사 텍스트(진입 시 랜덤 또는 고정 문구)
- 카드 테이블 선택 가능 여부(항상 true, MVP 기준)
- 일일 리워드 광고 버튼 상태: `{ enabled: Boolean, remainingToday: Int }` (FR-022/024)

### OrbitBetScreen
- 현재 보유 재화
- 베팅 금액 옵션 4종 + 각각 선택 가능 여부(잔고 기준, FR-013)
- 위험도 티어 3종 + 각 티어의 (플레이어에게 보여줄 수준의) 설명 — 최저 티어는 항상 선택 가능(FR-012a)
- GAME START 버튼 활성화 여부(금액+티어 모두 선택됐는지)

### OrbitGameScreen
- 상단: 매치 SIGNAL 현황(양쪽), 베팅 금액/티어 표시
- 중앙: 남은 드로우 덱 카드 수, **"방금 사용된 카드" 일회성 표시 이벤트**(지속 로그 아님 — FR-004)
- 하단: 플레이어 손패(1~2장, 카드별 이름/Power/짧은 효과 설명), 카드 선택 가능 여부(SHIELD 무효화·AI_CORE 강제 사용 등 규칙 반영)
- 카드 사용 시 필요한 추가 입력 UI: SCOUT_DRONE(Power 추측, 1 제외), EMP/WARP_GATE(대상 선택 — 1v1이므로 자신/상대 선택지만 있으면 됨)
- SHIELD 보호 표시(자신 또는 상대)

### OrbitResultScreen
- 승/패 여부, 베팅액, 획득 보상 또는 손실액, 순변동
- "다시 하기" / "휴게실로" 액션

## UI → ViewModel 인텐트 (Intent)

- `EnterLounge`
- `ClaimDailyAdReward` → `ClaimOrbitDailyAdRewardUseCase` 호출, 결과에 따라 성공/실패 스낵바 등 표시
- `SelectBetAmount(amount)`
- `SelectRiskTier(tier)`
- `StartMatch` → `StartOrbitMatchUseCase` 호출
- `PlayCard(card, extra)` → `PlayOrbitCardUseCase` 호출 (extra: 추측값/대상 등)
- `LeaveMatch` (뒤로가기 등) → 매치 진행 중이면 패배로 확정(FR-016), 결과 화면 없이 즉시 이탈 처리
- `RequestRules` → 설명 다이얼로그 표시(FR-021, 별도 튜토리얼 아님)
- `PlayAgain` / `ReturnToLounge`

## 계층 경계 원칙 (재확인)

- `OrbitRoundState.deck.usedCards`(도메인 내부, AI 판단용)는 어떤 State에도 "지속 열람 가능한 리스트" 형태로 그대로 노출하지 않는다. UI에는 오직 "방금 발생한 카드 사용" 1건짜리 이벤트만 전달한다(FR-004, research.md #6 참고).
- CPU 손패는 게임 진행 중 어떤 State에도 노출하지 않는다(플레이어가 Sensor로 확인했거나 매치 종료로 결과가 확정된 경우는 예외).
