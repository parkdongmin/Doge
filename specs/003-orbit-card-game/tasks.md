---

description: "Task list for ORBIT — Crew Lounge Card Mini-Game"
---

# Tasks: ORBIT — Crew Lounge Card Mini-Game

**Input**: Design documents from `/specs/003-orbit-card-game/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md (모두 존재)

**Tests**: 기획 원문 16번 항목("UI 제작 전에 Kotlin Unit Test만으로 게임 전체가 정상 동작해야 한다")과 plan.md의 Testing 전략에 따라, 도메인 로직(US1 카드 규칙, US2 정산, US4 일일 광고 게이트)에는 구현 전에 유닛 테스트 작성을 포함한다.

**Organization**: 작업은 spec.md의 사용자 스토리(P1~P4)별로 그룹화되어, 각 스토리를 독립적으로 구현·검증할 수 있다.

## Path Conventions

기존 단일 Android 앱 모듈(`app`) 기준. Kotlin 소스는 `app/src/main/java/com/doge/simulator/...`, 유닛 테스트는 `app/src/test/java/com/doge/simulator/...` (plan.md Project Structure 참고).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: 신규 기능이 기존 빌드/상수/내비게이션 인프라에 붙을 자리를 마련

- [x] T001 [P] ORBIT 튜닝 상수(고정 베팅 금액 4종: 500/1,000/2,500/5,000, 위험도 티어 3단계용 실수빈도·보상배율 placeholder, 일일 리워드 광고 최대 횟수=5·1회 보상=500) 추가 in `app/src/main/java/com/doge/simulator/domain/model/GameConstants.kt`
- [x] T002 [P] `AD_UNIT_REWARD_ORBIT_DAILY` buildConfigField 추가(기존 `AD_UNIT_REWARD_*` 패턴과 동일하게 디버그는 Google 테스트 광고 ID 고정) in `app/build.gradle.kts`
- [x] T003 [P] `NavRoutes`에 Lounge/OrbitBet/OrbitGame/OrbitResult 라우트 상수 추가 in `app/src/main/java/com/doge/simulator/presentation/navigation/NavRoutes.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: US1·US2 도메인 로직이 함께 참조하는 최소 공유 타입

**⚠️ CRITICAL**: 이 단계 완료 전에는 US1/US2 구현을 시작할 수 없음

- [x] T004 [P] `PlayerSide`(PLAYER, B01) enum 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/PlayerSide.kt`
- [x] T005 [P] `RoundEndReason`(OUT, DECK_EXHAUSTED, DRAW), `MatchResult`(WON, LOST) enum 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitEnums.kt`

**Checkpoint**: 이후 US1(카드 엔진)과 US2(베팅)를 병행 착수할 수 있는 기반 완료

---

## Phase 3: User Story 1 - 카드 매치 완주 (Priority: P1) 🎯 MVP

**Goal**: 베팅이나 휴게실 연동 없이, 카드 게임 화면 하나만으로 B-01과 매치를 시작해 SIGNAL 3개 선취(또는 패배)까지 완주할 수 있다.

**Independent Test**: 베팅/정거장 연동 없이 `OrbitGameScreen`을 단독 실행해 매치 시작→여러 라운드 진행→SIGNAL 3개 선취 또는 패배까지 끝까지 진행해 결과를 확인한다.

### Tests for User Story 1 ⚠️

> 아래 테스트를 먼저 작성하고 실패하는 것을 확인한 뒤 구현을 시작한다.

- [x] T006 [P] [US1] `OrbitDeck` 셔플/드로우/17장 매수/사용카드 내부추적 유닛 테스트 in `app/src/test/java/com/doge/simulator/domain/model/orbit/OrbitDeckTest.kt`
- [x] T007 [P] [US1] 8종 카드 이펙트(SCOUT_DRONE 정답/오답, SENSOR 열람, PROBE 비교, SHIELD로 인한 5종 효과 무효화, EMP 자기/상대 대상+덱 0장 시 사용불가, WARP_GATE 교환+CAPTAIN 예외, AI_CORE 강제사용, CAPTAIN 직접사용/EMP폐기 시 OUT) 유닛 테스트 in `app/src/test/java/com/doge/simulator/domain/usecase/orbit/PlayOrbitCardUseCaseTest.kt`
- [x] T008 [P] [US1] 덱 소진 3단계 비교(마지막 손패 Power → 라운드 사용 카드 Power 총합 → DRAW) 및 SIGNAL 3 선취 매치 종료 유닛 테스트 in `app/src/test/java/com/doge/simulator/domain/usecase/orbit/ResolveOrbitRoundEndUseCaseTest.kt`
- [x] T009 [P] [US1] `B01OrbitAi`가 플레이어의 실제 손패를 참조하지 않고 공개 정보(사용된 카드/열람·교환 이력/SHIELD 상태)만으로 판단하는지 검증하는 유닛 테스트 in `app/src/test/java/com/doge/simulator/domain/ai/B01OrbitAiTest.kt`

### Implementation for User Story 1

- [x] T010 [P] [US1] `OrbitCardType` enum(8종 각각의 Power·매수) 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitCardType.kt`
- [x] T011 [P] [US1] `OrbitCard` 데이터 클래스 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitCard.kt`
- [x] T012 [US1] `OrbitDeck`(셔플/드로우/사용된 카드 내부 추적 — AI 전용, UI 미노출) 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitDeck.kt` (depends on T010, T011; T006 통과 목표)
- [x] T013 [P] [US1] `OrbitPlayerState`(손패/OUT 여부/SHIELD 보호+만료) 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitPlayerState.kt`
- [x] T014 [US1] `OrbitRoundState`/`OrbitMatchState`(SIGNAL 누적, 라운드 종료 사유) 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitRoundState.kt`, `OrbitMatchState.kt` (depends on T004, T005, T012, T013)
- [x] T015 [US1] `PlayOrbitCardUseCase`(턴당 드로우+카드 사용+8종 이펙트+SHIELD 무효화 규칙+AI_CORE 강제사용 규칙 적용) 구현 in `app/src/main/java/com/doge/simulator/domain/usecase/orbit/PlayOrbitCardUseCase.kt` (depends on T014; T007 통과 목표)
- [x] T016 [US1] `ResolveOrbitRoundEndUseCase`(OUT 즉시 종료, 덱 소진 3단계 비교, DRAW 시 라운드 재시작, SIGNAL 3 선취 매치 종료) 구현 in `app/src/main/java/com/doge/simulator/domain/usecase/orbit/ResolveOrbitRoundEndUseCase.kt` (depends on T014; T008 통과 목표)
- [x] T017 [US1] `B01OrbitAi`(공개 정보 기반 판단 + `mistakeRate` 파라미터로 차선책을 일정 확률 선택) 구현 in `app/src/main/java/com/doge/simulator/domain/ai/B01OrbitAi.kt` (depends on T014; T009 통과 목표)
- [x] T018 [US1] `OrbitViewModel` 초안(매치 상태 보유, `PlayCard` 인텐트 처리, "방금 사용된 카드"는 일회성 이벤트로만 노출하고 지속 로그는 제공하지 않음) 구현 in `app/src/main/java/com/doge/simulator/presentation/viewmodel/OrbitViewModel.kt` (depends on T015, T016, T017)
- [x] T019 [US1] `OrbitGameScreen`(손패 2장/덱 잔여 수/SIGNAL 표시/카드 일시 공개 연출, 지속 로그 UI 없음) 구현 in `app/src/main/java/com/doge/simulator/presentation/screen/orbit/OrbitGameScreen.kt` (depends on T018)

**Checkpoint**: 이 시점에서 US1은 베팅·정거장 연동 없이 독립적으로 완전히 동작·검증 가능해야 한다.

---

## Phase 4: User Story 2 - 베팅과 재화 반영 (Priority: P2)

**Goal**: 매치 시작 전 베팅 금액과 위험도 티어를 선택하고, 매치 결과(승/패/이탈)에 따라 재화가 규칙대로 반영된다.

**Independent Test**: 베팅 화면에서 금액+티어를 선택해 매치를 시작하고, 승리/패배/중도 이탈 각각으로 매치를 종료시켜 매번 재화 잔고 변화를 확인한다(US1의 카드 엔진 위에서 검증).

### Tests for User Story 2 ⚠️

- [x] T020 [P] [US2] `OrbitRiskTier` 3단계의 실수빈도/보상배율 매핑 및 "최저 티어는 항상 선택 가능" 규칙 유닛 테스트 in `app/src/test/java/com/doge/simulator/domain/model/orbit/OrbitRiskTierTest.kt`
- [x] T021 [P] [US2] `SettleOrbitBetUseCase`(승리 시 티어별 보상배율 지급, 패배·이탈 시 이미 차감된 베팅액 환불 없이 손실 확정) 유닛 테스트 in `app/src/test/java/com/doge/simulator/domain/usecase/orbit/SettleOrbitBetUseCaseTest.kt`

### Implementation for User Story 2

- [x] T022 [P] [US2] `OrbitRiskTier` enum(3단계: `aiMistakeRate`, `winRewardMultiplier`, 최저 티어 항상 노출) 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitRiskTier.kt`
- [x] T023 [US2] `OrbitBet` 데이터 클래스(금액 4단계 + 위험도 티어 + 정산 결과) 구현 in `app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitBet.kt` (depends on T022)
- [x] T024 [US2] `StartOrbitMatchUseCase`(잔고 ≥ 베팅액 확인 → 즉시 차감 → 매치 생성, 선택된 티어의 `aiMistakeRate`를 `B01OrbitAi`에 주입) 구현 in `app/src/main/java/com/doge/simulator/domain/usecase/orbit/StartOrbitMatchUseCase.kt` (depends on T023, T014, T017; 기존 재화 저장소 연동)
- [x] T025 [US2] `SettleOrbitBetUseCase`(승리 보상/패배·이탈 손실 확정, 기존 재화 저장소 반영) 구현 in `app/src/main/java/com/doge/simulator/domain/usecase/orbit/SettleOrbitBetUseCase.kt` (depends on T023; T021 통과 목표)
- [x] T026 [US2] `OrbitBetScreen`(현재 잔고 표시, 금액 4단계 중 잔고 미만 옵션 비활성화, 위험도 티어 3단계 선택, GAME START) 구현 in `app/src/main/java/com/doge/simulator/presentation/screen/orbit/OrbitBetScreen.kt` (depends on T024)
- [x] T027 [US2] `OrbitViewModel`에 `SelectBetAmount`/`SelectRiskTier`/`StartMatch`/`LeaveMatch`(이탈=패배 확정) 인텐트 추가 in `app/src/main/java/com/doge/simulator/presentation/viewmodel/OrbitViewModel.kt` (depends on T018, T024, T025)
- [x] T028 [US2] `OrbitResultScreen`(승/패, 베팅액/획득 보상 또는 손실, 순변동 표시, 다시하기/휴게실로 액션) 구현 in `app/src/main/java/com/doge/simulator/presentation/screen/orbit/OrbitResultScreen.kt` (depends on T025)

**Checkpoint**: US1+US2 통합 시 베팅→매치→결과 전 과정에서 재화가 스펙대로 반영되어야 한다.

---

## Phase 5: User Story 3 - 휴게실을 통한 접근 (Priority: P3)

**Goal**: 정거장에서 휴게실을 거쳐 카드 테이블→베팅→게임→결과→휴게실로 이어지는 전체 진입/복귀 경로가 자연스럽게 연결된다.

**Independent Test**: 정거장 탭에서 시작해 휴게실 → 카드 테이블 → 베팅 화면까지, 그리고 결과 화면에서 휴게실 복귀까지의 흐름만 별도로 검증한다.

### Implementation for User Story 3

- [x] T029 [P] [US3] `LoungeScreen`(B-01 대사, 카드 테이블 진입점, 리워드 광고 버튼 슬롯) 구현 in `app/src/main/java/com/doge/simulator/presentation/screen/orbit/LoungeScreen.kt`
- [x] T030 [US3] `HQScreen`에 "휴게실" 진입점 추가 및 Lounge 라우트 연결 in `app/src/main/java/com/doge/simulator/presentation/screen/hq/HQScreen.kt` (depends on T003, T029)
- [x] T031 [US3] 정거장→휴게실→카드테이블→베팅→게임→결과→휴게실 전체 내비게이션 그래프 연결 in `app/src/main/java/com/doge/simulator/presentation/navigation/`(기존 NavHost 파일) (depends on T026, T019, T028, T030)
- [x] T032 [US3] ORBIT 게임(`OrbitGameScreen`) 진행 중 기존 BottomNavigation 숨김 처리 in 해당 Scaffold/NavHost 파일 (depends on T031)
- [x] T033 [P] [US3] 전체 규칙 요약 + 카드별 역할/효과 설명 다이얼로그(기존 ⓘ 스타일, 해요체, BodyReading 폰트 — 단계별 튜토리얼 아님) 구현 in `app/src/main/java/com/doge/simulator/presentation/screen/orbit/OrbitRulesDialog.kt`

**Checkpoint**: 정거장에서 시작해 휴게실→베팅→게임→결과→복귀까지 끊김 없이 전체 흐름이 동작해야 한다.

---

## Phase 6: User Story 4 - 일일 리워드 광고 (Priority: P4)

**Goal**: 휴게실에서 잔고와 무관하게 하루 최대 5회 리워드 광고로 재화 500을 받을 수 있고, 로드 실패/도중 이탈 시 불이익이 없다.

**Independent Test**: 휴게실에서 리워드 광고 버튼을 반복 사용해 하루 5회까지 재화가 지급되고, 6번째부터 다음 날 로컬 자정까지 비활성화되는지 확인한다.

### Tests for User Story 4 ⚠️

- [x] T034 [P] [US4] `OrbitDailyRewardGate`의 일 5회 제한, 로컬 자정 리셋, 실패/이탈 시 카운트 미차감 유닛 테스트 in `app/src/test/java/com/doge/simulator/ads/OrbitDailyRewardGateTest.kt`

### Implementation for User Story 4

- [x] T035 [P] [US4] `OrbitDailyRewardGate`(SharedPreferences 기반 `viewedCount` + `lastResetLocalDate`, 조회 시 로컬 자정 경과分 리셋) 구현 in `app/src/main/java/com/doge/simulator/ads/OrbitDailyRewardGate.kt`(`AdFrequencyGate` 패턴 재사용; T034 통과 목표)
- [x] T036 [US4] `RewardedAdManager`에 `RewardPlacement.ORBIT_DAILY` 추가 및 `adUnitIdFor` 매핑 in `app/src/main/java/com/doge/simulator/ads/RewardedAdManager.kt` (depends on T002)
- [x] T037 [US4] `ClaimOrbitDailyAdRewardUseCase`(`RewardedAdResult.Earned`일 때만 재화 500 지급+카운트 증가, `Dismissed`/`Failed`/`NotReady`는 상태 불변) 구현 in `app/src/main/java/com/doge/simulator/domain/usecase/orbit/ClaimOrbitDailyAdRewardUseCase.kt` (depends on T035, T036)
- [x] T038 [US4] `LoungeScreen`에 리워드 광고 버튼 상태(남은 횟수/비활성화 표시) 연결 in `app/src/main/java/com/doge/simulator/presentation/screen/orbit/LoungeScreen.kt` (depends on T029, T037)
- [x] T039 [US4] `OrbitViewModel`(또는 Lounge 전용 ViewModel)에 `ClaimDailyAdReward` 인텐트 추가 in `app/src/main/java/com/doge/simulator/presentation/viewmodel/OrbitViewModel.kt` (depends on T037)

**Checkpoint**: 휴게실에서 잔고와 무관하게 하루 5회까지 리워드 광고로 재화를 확보할 수 있고, 실패 케이스가 플레이어에게 불이익을 주지 않아야 한다.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T040 [P] `quickstart.md`에 명시된 도메인 유닛 테스트 전체 통과 확인 (`./gradlew testDebugUnitTest --tests "com.doge.simulator.domain.*.orbit.*"` 등)
- [x] T041 매치 종료 후 전면광고 노출 지점을 `OrbitViewModel`에 훅으로만 표시(정확한 노출 빈도는 spec.md Assumptions에 따라 이번 범위에서 확정하지 않음) in `app/src/main/java/com/doge/simulator/presentation/viewmodel/OrbitViewModel.kt`
- [ ] T042 `quickstart.md`의 수동 QA 체크리스트(정거장→휴게실→베팅→게임→결과→복귀, 이탈=패배, 잔고부족 베팅 비활성화, 리워드 광고 5회 제한) 전체 수행

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음 — 즉시 시작 가능
- **Foundational (Phase 2)**: Setup 완료 후 시작. US1·US2 구현을 막는 선행 조건
- **User Story 1 (Phase 3)**: Foundational 완료 후 시작 가능. 다른 스토리에 의존하지 않음
- **User Story 2 (Phase 4)**: Foundational 완료 후 시작 가능하나, 매치를 실제로 실행해야 정산을 검증할 수 있어 **US1 완료 후 진행 권장**(엄밀한 코드 의존은 T024/T025가 US1의 `OrbitMatchState`/`B01OrbitAi`를 사용하는 지점)
- **User Story 3 (Phase 5)**: US1(`OrbitGameScreen`)과 US2(`OrbitBetScreen`, `OrbitResultScreen`)가 존재해야 전체 내비게이션 그래프(T031)를 연결할 수 있음 — US1·US2 완료 후 진행
- **User Story 4 (Phase 6)**: Foundational 이후 언제든 독립적으로 진행 가능(카드 엔진에 의존하지 않음). 다만 버튼을 배치할 `LoungeScreen`(T029, US3)이 있어야 화면에 연결(T038) 가능
- **Polish (Phase 7)**: 구현하기로 한 모든 스토리 완료 후 진행

### Parallel Opportunities

- Setup의 T001~T003은 모두 [P] — 병행 가능
- Foundational의 T004~T005는 [P] — 병행 가능
- US1 테스트 T006~T009는 [P] — 병행 작성 가능
- US1 모델 T010~T011, T013은 [P] — 병행 가능
- US2 테스트 T020~T021, 모델 초기 T022는 [P]
- US4는 US1/US2와 병행 착수 가능(카드 엔진에 의존하지 않음) — Foundational 완료 직후 별도 개발자가 US4를 바로 시작할 수 있음
- US3의 T029, T033은 [P]

---

## Parallel Example: User Story 1

```bash
# US1 테스트를 함께 착수
Task: "OrbitDeck 유닛 테스트 in app/src/test/java/com/doge/simulator/domain/model/orbit/OrbitDeckTest.kt"
Task: "8종 카드 이펙트 유닛 테스트 in app/src/test/java/com/doge/simulator/domain/usecase/orbit/PlayOrbitCardUseCaseTest.kt"
Task: "덱 소진/DRAW/SIGNAL 유닛 테스트 in app/src/test/java/com/doge/simulator/domain/usecase/orbit/ResolveOrbitRoundEndUseCaseTest.kt"
Task: "B01OrbitAi 정보 비대칭 유닛 테스트 in app/src/test/java/com/doge/simulator/domain/ai/B01OrbitAiTest.kt"

# US1 모델을 함께 착수
Task: "OrbitCardType enum in app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitCardType.kt"
Task: "OrbitCard 데이터 클래스 in app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitCard.kt"
Task: "OrbitPlayerState in app/src/main/java/com/doge/simulator/domain/model/orbit/OrbitPlayerState.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup 완료
2. Phase 2 Foundational 완료
3. Phase 3 User Story 1 완료 — 이 시점에 카드 게임 로직 전체가 유닛 테스트로 검증된 상태
4. **STOP and VALIDATE**: `OrbitGameScreen`을 단독 실행해 베팅 없이 매치를 완주할 수 있는지 확인
5. 이 상태로도 기획 원문 16번 항목이 요구한 "UI 전에 게임 전체가 정상 동작"이라는 목표는 달성됨

### Incremental Delivery

1. Setup + Foundational → 기반 완료
2. US1 추가 → 독립 검증 → (내부) 데모 가능(MVP 카드 로직)
3. US2 추가 → 독립 검증 → 베팅까지 포함한 완전한 한 판 흐름 완성
4. US3 추가 → 독립 검증 → 정거장에서 실제로 도달 가능한 완성된 기능
5. US4 추가 → 독립 검증 → 파산 안전망까지 포함한 최종 범위 완성
6. Phase 7 Polish

### Team Strategy

Foundational 완료 후:
- 개발자 A: US1(카드 엔진, 가장 규모가 큼)
- 개발자 B: US4(카드 엔진과 독립적이므로 바로 착수 가능)
- US1이 어느 정도 진행된 뒤 개발자 C가 US2 착수(매치 실행 로직에 의존)
- US1+US2 완료 후 US3(내비게이션 통합) 착수

---

## Notes

- [P] 작업 = 서로 다른 파일, 선행 미완료 작업에 의존하지 않음
- [Story] 라벨은 spec.md의 사용자 스토리(US1~US4)에 대응
- 정확한 위험도 티어 수치(`aiMistakeRate`/`winRewardMultiplier`)와 광고 노출 빈도는 spec.md Assumptions에 따라 이번 태스크 범위에서 확정하지 않고 placeholder 값으로 구현한다
- 테스트를 먼저 작성하고 실패를 확인한 뒤 구현할 것(US1/US2/US4의 각 Tests 섹션)
- 논리적 작업 단위(태스크 또는 스토리 완료)마다 커밋할 것
