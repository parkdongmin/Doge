# Phase 0 Research: ORBIT — Crew Lounge Card Mini-Game

기술 스택 자체는 기존 Doge 앱(Kotlin/Compose/Hilt/AdMob)을 그대로 재사용하므로 외부 기술 리서치가 필요한 항목은 없다. 대신 스펙(`spec.md`)의 요구사항을 만족시키기 위해 내려야 했던 설계 결정들을 정리한다.

## 1. 게임 로직의 위치: 순수 Kotlin 도메인 계층

- **Decision**: Card/Deck/Round/Match/B-01 AI 로직 전체를 Android·Compose 의존이 없는 순수 Kotlin(`domain/model/orbit`, `domain/ai`, `domain/usecase/orbit`)으로 구현한다.
- **Rationale**: 기획 원문 16번 항목이 "UI 제작 전에 Kotlin Unit Test만으로 게임 전체가 정상 동작해야 한다"를 명시적으로 요구하며, 기존 프로젝트의 `domain/usecase` 계층도 이미 이 원칙(Android 프레임워크 비의존)을 따르고 있음(`GenerateExpeditionReportUseCase` 등).
- **Alternatives considered**: ViewModel 내부에 게임 로직을 직접 구현 — Compose 상태와 강하게 결합되어 유닛 테스트가 어려워지고, 화면 재작업 시 로직까지 함께 흔들릴 위험이 커서 기각.

## 2. 매치/라운드 상태 영속화 여부

- **Decision**: 진행 중인 매치·라운드 상태는 영속화하지 않고 화면/프로세스 생명주기 동안의 메모리 상태로만 유지한다.
- **Rationale**: 스펙 US2/FR-016이 "매치 도중 이탈은 패배로 확정, 베팅액 환불 없음"으로 이미 결정했고, 재개(resume) 요구사항이 없다. Room에 진행 중 매치를 저장하면 스키마/마이그레이션 비용만 늘고 스펙이 요구하지 않는 기능이 된다.
- **Alternatives considered**: Room에 매치 스냅샷 저장 후 재개 지원 — 스펙 범위를 벗어나 기각. 다만 재화(지갑) 차감/지급은 매치 시작·종료 시점에 기존 영속 저장소에 반영되므로 재화 자체는 안전하다.

## 3. 일일 리워드 광고 카운터 저장 방식

- **Decision**: 기존 `AdFrequencyGate`와 동일하게 `SharedPreferences`에 "오늘 시청 횟수"와 "마지막 리셋 날짜(로컬 `yyyy-MM-dd`)"를 저장하고, 조회 시 오늘 날짜와 다르면 카운트를 0으로 리셋한다.
- **Rationale**: 스펙에서 "기기 로컬 시간 자정 리셋, 클라우드 미동기화"로 명시적으로 확정(FR-024/025). 기존 광고 빈도 제어 코드(`AdFrequencyGate`)가 이미 동일한 패턴(SharedPreferences 기반 로컬 카운터)을 사용 중이라 그대로 재사용 가능.
- **Alternatives considered**: 롤링 24시간 쿨다운 방식(기존 `AdFrequencyGate`의 전면광고 쿨다운과 동일한 방식) — 스펙이 명시적으로 "로컬 자정" 기준을 요구하므로 채택하지 않음.

## 4. 리워드 광고 시청 결과 처리

- **Decision**: 기존 `RewardedAdManager`의 `RewardedAdResult`(Earned/NotReady/Dismissed/Failed) 모델을 그대로 사용하고, `Earned`일 때만 재화 지급 + 카운터 증가를 수행한다. `Dismissed`/`Failed`/`NotReady`는 아무 것도 하지 않는다.
- **Rationale**: FR-026("로드 실패·도중 이탈 시 미지급, 횟수 미차감")과 기존 광고 매니저의 결과 타입이 정확히 대응되어 별도 예외 처리 로직이 필요 없다.
- **Alternatives considered**: 별도의 ORBIT 전용 콜백 타입 정의 — 기존 타입으로 충분히 표현 가능해 불필요한 중복으로 판단, 기각.

## 5. 위험도(배율) 티어와 CPU 실수 빈도의 연결

- **Decision**: `OrbitRiskTier`를 "실수 빈도(mistakeRate: Float)"와 "승리 보상 배율(rewardMultiplier: Float)"을 함께 갖는 데이터로 모델링하고, `B01OrbitAi`는 이 값을 파라미터로 받아 매 판단마다 "최적 수"와 "일정 확률로 대체되는 차선 수" 중 하나를 고른다.
- **Rationale**: 스펙 FR-011/FR-012a가 "정확한 계산은 항상 하되 일부러 최적이 아닌 선택을 섞는 빈도로 난이도를 구현"하도록 명시. 티어별로 이 빈도만 다르게 주입하면 AI 로직 자체는 하나만 구현하면 됨.
- **Alternatives considered**: 티어마다 별도의 AI 클래스 작성 — 로직 대부분이 중복되고 유지보수 비용이 커서 기각. 확률 계산 정확도 자체를 낮추는 방식 — 세션 논의에서 "덱이 17장뿐이라 계산 정확도를 낮추면 오히려 부자연스럽다"는 이유로 이미 기각됨(spec Assumptions 참고).

## 6. 카드 사용 정보의 "일시적 공개, 비영속 로그" 원칙 구현

- **Decision**: 도메인 상태(`OrbitRoundState`)는 "지금까지 사용된 카드 목록"을 계속 내부적으로 들고 있어야 하지만(그렇지 않으면 B-01 AI가 덱 카운팅을 할 수 없음), 이 목록을 프레젠테이션 계층(ViewModel/UI State)에는 지속적으로 노출하지 않고 "방금 사용된 카드 1건"에 대한 일회성 이벤트로만 UI에 전달한다.
- **Rationale**: FR-004/FR-010이 요구하는 "AI는 정확히 기억, 플레이어 UI에는 지속 로그 없음"이라는 의도된 비대칭을 계층 경계로 구현한다. 도메인 계층에서 정보 자체를 지우면 AI 로직이 스펙을 만족할 수 없으므로, 정보는 유지하되 노출 계층에서 걸러낸다.
- **Alternatives considered**: 도메인에서도 사용 즉시 정보를 버림 — AI가 카드 카운팅을 할 수 없어 FR-010 위반. UI에 전체 로그를 노출하고 "안 보는 건 사용자 재량" — FR-004가 요구하는 "다시 조회 불가"를 UI 레벨에서 보장하지 못해 기각.

## 7. 베팅 금액과 위험도 티어를 독립적인 축으로 모델링

- **Decision**: 베팅 금액(4단계 고정)과 위험도 티어(3단계)를 각각 별도의 enum/데이터 클래스로 정의하고, `OrbitBet`이 둘을 함께 참조하도록 조합한다.
- **Rationale**: 스펙 Clarifications 세션에서 두 축이 명시적으로 분리되어 확정됨(금액은 고정, 티어가 난이도+배율을 결정).
- **Alternatives considered**: 금액×티어 조합을 미리 나열한 단일 목록(12종) — 금액이나 배율값 조정 시 전체 목록을 다시 나열해야 해 확장성이 떨어져 기각.

## Output

모든 NEEDS CLARIFICATION 항목 해결됨. Technical Context에 남은 미확정 항목 없음 (스펙 Assumptions에 명시된 "정확한 수치"류 항목은 경제 밸런싱 단계로 의도적으로 이관됨).
